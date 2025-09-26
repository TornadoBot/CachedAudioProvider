package bot.tornado.cachedaudioprovider.service.extraction;

import bot.tornado.cachedaudioprovider.dto.YtDlpEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Service responsible for extracting audio metadata from YouTube and YouTube Music using yt-dlp.
 *
 * <p>Encapsulates three main responsibilities:
 * <ul>
 *     <li>Building yt-dlp commands via {@link YtDlpProcessFactory}</li>
 *     <li>Executing commands and capturing JSON output via {@link YtDlpExecutor}</li>
 *     <li>Parsing results into {@link YtDlpEntry} objects via {@link YtDlpParser}</li>
 * </ul>
 *
 * <p>Supports extraction by:
 * <ul>
 *     <li>Direct video URL (e.g. YouTube/YouTube Music)</li>
 *     <li>Free-form search query</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YtDlpService {
    private final YtDlpProcessFactory processFactory;
    private final YtDlpExecutor executor;
    private final YtDlpParser parser;

    /**
     * Extracts metadata from a direct video URL.
     *
     * <p>This method uses yt-dlp to download structured metadata for a single video.
     * If successful, it parses the result into a {@link YtDlpEntry}.
     *
     * @param url a full YouTube or YouTube Music video URL
     * @return an {@link Optional} containing the metadata, or empty if the process or parsing failed
     */
    public Optional<YtDlpEntry> extractByUrl(String url) {
        ProcessBuilder builder = this.processFactory.buildAudioExtractionCommandForUrl(url);
        Optional<String> json = this.executor.executeAndCaptureJson(builder, Duration.ofSeconds(30));
        return json.flatMap(this.parser::parseSingleEntry);
    }

    /**
     * Extracts metadata for the top 3 results from a search query on YouTube Music.
     *
     * <p>This method is typically used for free-form input like
     * {@code "artist + song name"} and returns up to 3 results.
     *
     * @param search the search query string (e.g. "Muse Uprising")
     * @return a list of matching entries may be empty if nothing was found or failed
     */
    public List<YtDlpEntry> extractBySearchQuery(String search) {
        ProcessBuilder builder = this.processFactory.buildSearchResultsExtractionForYTMusic(search);
        Optional<String> json = this.executor.executeAndCaptureJson(builder, Duration.ofSeconds(30));
        return json.map(this.parser::parseMultipleEntries).orElse(List.of());
    }
}
