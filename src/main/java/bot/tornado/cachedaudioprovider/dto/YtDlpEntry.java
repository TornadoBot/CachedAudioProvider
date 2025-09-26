package bot.tornado.cachedaudioprovider.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Represents a single yt-dlp entry as parsed from --dump-single-json output.
 * This DTO can describe either a standalone video or a playlist item.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class YtDlpEntry {
    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("uploader")
    private String uploader;

    @JsonProperty("channel_url")
    private String channelUrl;

    @JsonProperty("duration")
    private Integer duration;

    @JsonProperty("like_count")
    private Integer likeCount;

    @JsonProperty("view_count")
    private Integer viewCount;

    @JsonProperty("timestamp")
    private Long uploadTimestamp;

    @JsonProperty("artist")
    private String artist;

    @JsonProperty("track")
    private String track;

    @JsonProperty("album")
    private String album;
}
