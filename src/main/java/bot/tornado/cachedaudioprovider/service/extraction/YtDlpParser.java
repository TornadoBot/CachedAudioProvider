package bot.tornado.cachedaudioprovider.service.extraction;

import bot.tornado.cachedaudioprovider.dto.YtDlpEntry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Parses JSON output from yt-dlp to one or more {@link YtDlpEntry} objects.
 *
 * <p>This component supports both single video results (e.g., direct video download)
 * and playlist-style results (e.g., search results, playlists, albums).
 *
 * <p>Uses Jackson's {@link ObjectMapper} to deserialize yt-dlp output.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YtDlpParser {
    private final ObjectMapper objectMapper;

    /**
     * Parses a single {@link YtDlpEntry} from the provided yt-dlp JSON string.
     *
     * <p>If the result is a playlist or contains multiple entries, only the first valid
     * entry is returned.
     *
     * @param rawJson the raw JSON string output from yt-dlp
     * @return an {@link Optional} containing a parsed {@link YtDlpEntry}, or empty if parsing fails
     */
    public Optional<YtDlpEntry> parseSingleEntry(String rawJson) {
        List<YtDlpEntry> entries = extractEntries(rawJson);
        return entries.isEmpty() ? Optional.empty() : Optional.of(entries.getFirst());
    }

    /**
     * Parses a list of {@link YtDlpEntry} objects from a playlist or search result.
     *
     * <p>If the JSON only contains a single video, the result will contain one element.
     *
     * @param rawJson the raw JSON string output from yt-dlp
     * @return a list of parsed {@link YtDlpEntry} objects; empty if parsing fails
     */
    public List<YtDlpEntry> parseMultipleEntries(String rawJson) {
        return extractEntries(rawJson);
    }

    /**
     * Internal method to extract one or more entries from yt-dlp JSON.
     *
     * <p>Handles both flat single-entry results and playlists/search results.
     *
     * @param rawJson the raw JSON string from yt-dlp
     * @return a list of parsed {@link YtDlpEntry} objects
     */
    private List<YtDlpEntry> extractEntries(String rawJson) {
        List<YtDlpEntry> results = new ArrayList<>();
        try {
            JsonNode root = this.objectMapper.readTree(rawJson);

            // If it's a list of entries (e.g., playlist or search result)
            if (root.has("entries") && root.get("entries").isArray()) {
                for (JsonNode entry : root.get("entries")) {
                    try {
                        results.add(this.objectMapper.treeToValue(entry, YtDlpEntry.class));
                    } catch (JsonProcessingException e) {
                        log.warn("Skipping invalid yt-dlp entry: {}", e.getMessage());
                    }
                }
            } else {
                // Single video result
                results.add(this.objectMapper.treeToValue(root, YtDlpEntry.class));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to parse yt-dlp result: {}", e.getMessage());
        }
        return results;
    }
}
