package bot.tornado.cachedaudioprovider.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SongResponse {
    private String youtubeId;
    private String title;
    private String artist;
    private Integer duration;
    private String streamUrl;
}
