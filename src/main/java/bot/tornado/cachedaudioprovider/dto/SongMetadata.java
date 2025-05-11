package bot.tornado.cachedaudioprovider.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SongMetadata {
    private final String youtubeId;
    private final String title;
    private final String artist;
    private final String channelUrl;
    private final Integer duration;
    private final Integer likeCount;
    private final Integer viewCount;
    private final Instant uploadDate;
}
