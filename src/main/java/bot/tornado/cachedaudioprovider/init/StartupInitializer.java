package bot.tornado.cachedaudioprovider.init;

import bot.tornado.cachedaudioprovider.config.StorageProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Slf4j
@RequiredArgsConstructor
public class StartupInitializer {
    private final StorageProperties storageProperties;

    @PostConstruct
    public void init() {
        this.createAudioDirectory();
        this.verifyBinary("yt-dlp", "--version");
        this.verifyBinary("ffmpeg", "-version");
    }

    private void createAudioDirectory() {
        try {
            Files.createDirectories(Path.of(this.storageProperties.getPath()));
            log.info("Audio storage directory ensured at: {}", this.storageProperties.getPath());
        } catch (IOException e) {
            throw new IllegalStateException("Could not create audio storage directory.", e);
        }
    }

    private void verifyBinary(String command, String versionArg) {
        try {
            Process process = new ProcessBuilder(command, versionArg)
                    .redirectErrorStream(true)
                    .start();
            String version = new String(process.getInputStream().readAllBytes()).split("\n")[0].trim();
            process.waitFor();
            log.info("{} available: {}", command, version);
        } catch (IOException | InterruptedException e) {
            log.error("{} is not available or failed to run: {}", command, e.getMessage());
            throw new IllegalStateException(command + " is required but not available.", e);
        }
    }
}
