package bot.tornado.cachedaudioprovider.service;

import bot.tornado.cachedaudioprovider.aop.EnsureYtDlpUpdated;
import bot.tornado.cachedaudioprovider.config.StorageProperties;
import bot.tornado.cachedaudioprovider.dto.SongMetadata;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class YtDlpService {
    private final StorageProperties storageProperties;

    @EnsureYtDlpUpdated
    public SongMetadata extractByYoutubeId(String youtubeId) {
        log.info("Starting extraction for YouTube ID: {}", youtubeId);

        Process process;
        try {
            process = this.createProcess(youtubeId);
        } catch (IOException e) {
            log.error("Failed to create yt-dlp process for ID: {}", youtubeId, e);
            throw new IllegalStateException("Failed to create yt-dlp process for ID: " + youtubeId, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread was interrupted while starting yt-dlp process", e);
            throw new IllegalStateException("yt-dlp process was interrupted", e);
        }

        AtomicReference<String> metadata = new AtomicReference<>();
        Consumer<String> inputConsumer = (String s) -> {
            if (s.matches("(.+;){7}.+")) {
                metadata.set(s);
            }
        };

        Thread outputThread = createInputConsumer(process.getInputStream(), inputConsumer);
        Thread errorThread = createInputConsumer(process.getErrorStream(), log::error);
        outputThread.start();
        errorThread.start();

        boolean finished;
        try {
            finished = process.waitFor(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Thread was interrupted while waiting for yt-dlp process to finish", e);
            throw new IllegalStateException("yt-dlp process was interrupted", e);
        }

        if (!finished) {
            log.error("yt-dlp process timed out for video ID: {}", youtubeId);
            process.destroyForcibly();
            throw new IllegalStateException("yt-dlp process timed out after 30 seconds");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            log.error("yt-dlp exited with code {} for video ID: {}", exitCode, youtubeId);
            throw new IllegalStateException("yt-dlp exited with non-zero exit code: " + exitCode);
        }

        String metadataString = metadata.get();
        if (metadataString == null) {
            log.error("yt-dlp failed to extract metadata for video ID: {}", youtubeId);
            throw new IllegalStateException("No metadata returned by yt-dlp");
        }
        log.info("Successfully extracted metadata for YouTube ID: {}", youtubeId);
        log.debug("Extracted metadata: {}", metadataString);
        return SongMetadata.fromDelimitedString(metadataString);
    }

    @EnsureYtDlpUpdated
    public SongMetadata extractBySearch(String search) {
        return null;  // TODO: Implement
    }

    @EnsureYtDlpUpdated
    public SongMetadata extractByTitleAndArtist(String title, String artist) {
        return null; // TODO: Implement.
    }

    private static Thread createInputConsumer(InputStream inputStream, Consumer<String> outputCallback) {
        return new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputCallback.accept(line);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, "YtDlpInputConsumer");
    }

    private Process createProcess(String videoId) throws IOException, InterruptedException {
        log.info("Creating yt-dlp process for {}", videoId);

        MetadataField[] fields = new MetadataField[] {
            new MetadataField("id", false),
            new MetadataField("title", true),
            new MetadataField("uploader", true),
            new MetadataField("channel_url", false),
            new MetadataField("duration", false),
            new MetadataField("like_count", false),
            new MetadataField("view_count", false),
            new MetadataField("timestamp", false)
        };

        String sep = ";";
        String sepEncoded = URLEncoder.encode(videoId, StandardCharsets.UTF_8);
        String query = String.join(sep, Arrays.stream(fields).map(MetadataField::toString).toArray(String[]::new));
        String replaceQuery = String.join(
            ",",
            Arrays.stream(fields)
                .filter(MetadataField::requiresCheck)
                .map(MetadataField::toString).toArray(String[]::new));
        String outputPath = "%s/%%(id)s".formatted(this.storageProperties.getPath());

        ProcessBuilder builder = new ProcessBuilder(
            "yt-dlp",
            "--format", "bestaudio/best",
            "--extract-audio",
            "--audio-format", "mp3",
            "--no-playlist",
            "--output", outputPath,
            "--print", "%s\n".formatted(query), "--no-simulate",
            "--replace-in-metadata", replaceQuery, "[%s]".formatted(sep), sepEncoded,
            "https://www.youtube.com/watch?v=%s".formatted(videoId) // TODO: ADD COOKIE
        );
        log.info("Starting yt-dlp process for {}", videoId);
        Process process = builder.start();
        log.debug("YtDlp process started for {}", videoId);
        return process;
    }

    private record MetadataField(String name, boolean requiresCheck) {
        @Override
        public @NonNull String toString() {
            return "%%(%s)s".formatted(this.name);
        }
    }
}
