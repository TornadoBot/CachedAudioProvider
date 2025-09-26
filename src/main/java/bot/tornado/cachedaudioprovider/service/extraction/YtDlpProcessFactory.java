package bot.tornado.cachedaudioprovider.service.extraction;

import bot.tornado.cachedaudioprovider.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Factory service responsible for constructing yt-dlp {@link ProcessBuilder} instances.
 *
 * <p>This component encapsulates the logic for configuring yt-dlp commands for:
 * <ul>
 *     <li>Extracting from generic URLs</li>
 *     <li>Searching and extracting top results from YouTube Music</li>
 * </ul>
 *
 * <p>It injects the {@link StorageProperties} to dynamically configure output directories.
 */
@Service
@RequiredArgsConstructor
public class YtDlpProcessFactory { // TODO: ADD COOKIE support
    private final StorageProperties storageProperties;

    /**
     * Builds a generic command to extract audio from a given URL using yt-dlp.
     *
     * <p>This is useful when the full video or music URL is already known.
     * Output is dumped as JSON and audio is downloaded as MP3.
     *
     * @param url the full video URL (e.g. YouTube, YouTube Music)
     * @return a configured {@link ProcessBuilder} instance
     */
    public ProcessBuilder buildAudioExtractionCommandForUrl(String url) {
        return new ProcessBuilder(
                "yt-dlp",
                "--format", "bestaudio/best",
                "--extract-audio",
                "--audio-format", "mp3",
                "--no-playlist",
                "--output", "%s/%%(id)s".formatted(this.storageProperties.getPath()),
                "--dump-single-json",
                "--no-simulate",
                url
        );
    }

    /**
     * Builds a command to search for the top 3 results on YouTube Music and extract metadata.
     *
     * <p>The results will be returned as a JSON playlist structure.
     * This does not download audio; it is used for fuzzy metadata matching.
     *
     * @param search the search query (e.g. "Imagine Dragons Believer")
     * @return a configured {@link ProcessBuilder} for YouTube Music search
     */
    public ProcessBuilder buildSearchResultsExtractionForYTMusic(String search) {
        String encodedSearch = URLEncoder.encode(search, StandardCharsets.UTF_8);
        return new ProcessBuilder(
                "yt-dlp",
                "--dump-single-json",
                "--playlist-items", "1:3",
                "https://music.youtube.com/search?q=%s#songs".formatted(encodedSearch)
        );
    }
}
