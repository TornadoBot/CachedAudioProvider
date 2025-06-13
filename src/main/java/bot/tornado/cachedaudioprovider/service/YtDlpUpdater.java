package bot.tornado.cachedaudioprovider.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Service responsible for updating the yt-dlp binary in the background at fixed intervals.
 * <p>
 * The update is triggered before every metadata extraction, but executes asynchronously and only if the last update is
 * older than {@link #UPDATE_INTERVAL}.
 * <p>
 * Thread-safe and non-blocking. Ensures that only one updater runs at a time.
 */
@Service
@Slf4j
public class YtDlpUpdater {
    /**
     * Minimum interval between consecutive yt-dlp update attempts.
     */
    private static final Duration UPDATE_INTERVAL = Duration.ofMinutes(15);

    /**
     * Timestamp of the last successful or attempted update.
     */
    private Instant lastUpdate = Instant.EPOCH;

    /**
     * Atomic lock to ensure only one update process runs at a time.
     */
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Triggers a background yt-dlp update if the last update is too old.
     * <p>
     * If an update is already running or the last update was recent,
     * this method exits immediately. Otherwise, it launches a background
     * thread to execute {@code yt-dlp -U} and logs the result.
     */
    public void updateIfDue() {
        if (Instant.now().isBefore(this.lastUpdate.plus(UPDATE_INTERVAL))) {
            return;
        }

        if (this.running.compareAndSet(false, true)) {
            new Thread(() -> {
                try {
                    log.info("Updating yt-dlp binaries...");
                    Process process = new ProcessBuilder("yt-dlp", "-U").start();
                    boolean finished = process.waitFor(10, TimeUnit.SECONDS);

                    if (!finished) {
                        log.warn("Failed to update yt-dlp binaries.");
                    } else {
                        this.lastUpdate = Instant.now();
                        log.info("Updated yt-dlp binaries.");
                    }
                } catch (InterruptedException | IOException e) {
                    Thread.currentThread().interrupt();
                    log.error("Failed to update yt-dlp binaries.", e);
                } finally {
                    this.running.set(false);
                }
            }, "yt-dlp-updater").start();
        }
    }
}
