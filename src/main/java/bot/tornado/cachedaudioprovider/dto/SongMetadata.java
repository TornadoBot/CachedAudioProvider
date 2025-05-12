package bot.tornado.cachedaudioprovider.dto;

import bot.tornado.cachedaudioprovider.util.Parser;
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

    public static SongMetadata fromDelimitedString(String metadata) {
        String[] split = metadata.split(";");
        for (int i = 0; i < split.length; i++) {
            split[i] = split[i].replaceAll("%3B", ";");
        }
        return SongMetadata.builder()
                .youtubeId(split[0])
                .title(split[1])
                .artist(split[2])
                .channelUrl(split[3])
                .duration(Parser.parseIntFailSave(split[4]))
                .likeCount(Parser.parseIntFailSave(split[5]))
                .viewCount(Parser.parseIntFailSave(split[6]))
                .uploadDate(Parser.parseUnixTimestamp(split[7]))
                .build();
    }
}
