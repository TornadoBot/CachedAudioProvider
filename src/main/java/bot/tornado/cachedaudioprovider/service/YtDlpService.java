package bot.tornado.cachedaudioprovider.service;

import bot.tornado.cachedaudioprovider.config.StorageProperties;
import bot.tornado.cachedaudioprovider.dto.SongMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Service
@Slf4j
@RequiredArgsConstructor
public class YtDlpService {
    private final StorageProperties storageProperties;

    public static SongMetadata extractBySearch(String search) {
        return null;
    }

    public SongMetadata extractByYoutubeId(String youtubeId) {
        Process process;
        try {
            process = this.createProcess(youtubeId);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("yt-dlp process timed out after 30 seconds");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("yt-dlp exited with code %d".formatted(exitCode));
        }

        String metadataString = metadata.get();
        if (metadataString == null) {
            throw new RuntimeException("yt-dlp failed to extract metadata");
        }
        return SongMetadata.fromDelimitedString(metadataString);
    }

    public SongMetadata extractByTitleAndArtist(String title, String artist) {
        return null;
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

    private Process createProcess(String videoId) throws IOException {
        String separator = ";";
        String[] metadata = {
                "id",
                "title",
                "uploader",
                "channel_url",
                "duration",
                "like_count",
                "view_count",
                "timestamp"
        };
        String[] metadataFields = new String[metadata.length];
        for (int i = 0; i < metadata.length; i++) {
            metadataFields[i] = "%%(%s)s".formatted(metadata[i]);
        }
        String template = "%s\n".formatted(String.join(separator, metadataFields));
        String outputPath = "%s/%%(id)s".formatted(this.storageProperties.getPath());

        return new ProcessBuilder(
                "yt-dlp",  // TODO: ADD UPDATE SUPPORT
                "--format", "bestaudio/best",
                "--extract-audio",
                "--audio-format", "mp3",
                "--no-playlist",
                "--output", outputPath,
                "--print", template, "--no-simulate",
                "--audio-quality", "0",
                "--replace-in-metadata", String.join(",", metadata), "[%s]".formatted(separator), "%3B",
                videoId // TODO: ADD COOKIE
        ).start();
    }
}
