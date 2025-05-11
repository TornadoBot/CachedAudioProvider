package bot.tornado.cachedaudioprovider.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SongRequest {
    private String youtubeId;
    private String spotifyId;
    private String search;
    private String title;
    private String artist;
}

