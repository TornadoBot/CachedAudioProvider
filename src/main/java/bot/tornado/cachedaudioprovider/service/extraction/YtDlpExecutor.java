package bot.tornado.cachedaudioprovider.service.extraction;

import bot.tornado.cachedaudioprovider.aop.EnsureYtDlpUpdated;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * Executes yt-dlp processes and extracts single-line JSON output.
 * This is used for both direct video extraction and search queries.
 * <p>
 * Automatically ensures yt-dlp is updated via {@link EnsureYtDlpUpdated} aspect.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YtDlpExecutor {
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Executes a yt-dlp command and returns the first valid JSON line from stdout.
     *
     * @param builder ProcessBuilder with yt-dlp command
     * @param timeout max duration to wait for process and output
     * @return Optional JSON string if successful; otherwise, empty
     */
    @EnsureYtDlpUpdated
    public Optional<String> executeAndCaptureJson(ProcessBuilder builder, Duration timeout) {
        Process process;
        try {
            log.debug("Executing yt-dlp command: {}", String.join(" ", builder.command()));
            process = builder.start();
        } catch (IOException e) {
            log.error("Error executing yt-dlp command", e);
            return Optional.empty();
        }

        AtomicReference<String> json = new AtomicReference<>();
        Thread outputThread = createConsumerThread(process, json, this::isValidJson);
        Thread errorThread = createErrorLoggerThread(process);

        outputThread.start();
        errorThread.start();

        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            outputThread.join(1000);
            if (outputThread.isAlive()) {
                log.warn("Output thread did not terminate as expected");
                outputThread.interrupt();
            }
            errorThread.join(500);
            if (errorThread.isAlive()) {
                log.warn("Error logger thread is still running after timeout");
                errorThread.interrupt();
            }

            if (!finished) {
                process.destroyForcibly();
                log.warn("yt-dlp process timed out");
                return Optional.empty();
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("yt-dlp exited with code {}", exitCode);
                return Optional.empty();
            }

            String output = json.get();
            if (output == null) {
                log.warn("No valid JSON output received");
                return Optional.empty();
            }

            output = output.trim();
            log.debug("yt-dlp output successfully captured ({} bytes)", output.length());
            return Optional.of(output);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("yt-dlp execution interrupted", e);
            return Optional.empty();
        }
    }

    /**
     * Creates a thread that reads the standard output of the given process and stores the first
     * line that satisfies the provided predicate (typically a valid JSON line) into the target reference.
     *
     * <p>This method is useful when you expect a single JSON output (e.g., from yt-dlp using
     * <code>--dump-single-json</code>) and want to asynchronously capture it without blocking the main thread.</p>
     *
     * <p>The thread stops reading as soon as a valid line is found. This avoids processing large
     * amounts of unnecessary output and ensures faster response time.</p>
     *
     * @param process the process whose stdout will be consumed
     * @param target the thread-safe reference that will hold the first valid line
     * @param filter a predicate that determines if a line is valid and should be captured
     * @return a new thread, not yet started, responsible for reading and capturing stdout
     */
    private static Thread createConsumerThread(
            Process process,
            AtomicReference<String> target,
            Predicate<String> filter
    ) {
        return new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (filter.test(line)) {
                        target.set(line);
                        break;
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read yt-dlp stdout", e);
            }
        }, "YtDlpStdout");
    }

    /**
     * Creates a thread that reads the error stream (stderr) of the given process
     * and logs each line as an error using SLF4J.
     *
     * <p>This is primarily for debugging and error diagnostics – yt-dlp writes useful warnings
     * and errors to stderr that can help trace problems like extraction failures or API issues.</p>
     *
     * <p>All lines are logged with the prefix "[yt-dlp]" and <code>ERROR</code> level.</p>
     *
     * @param process the process whose stderr will be consumed
     * @return a new thread, not yet started, responsible for logging stderr output
     */
    private static Thread createErrorLoggerThread(Process process) {
        return new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.error("[yt-dlp] {}", line);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read yt-dlp stderr", e);
            }
        }, YtDlpExecutor.class.getSimpleName() + "-stderr");
    }


    /**
     * Checks if the given string is valid JSON.
     *
     * @param json the candidate string
     * @return true if valid JSON, otherwise false
     */
    private boolean isValidJson(String json) {
        try {
            this.objectMapper.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }
}
