package bot.tornado.cachedaudioprovider.dto;

import bot.tornado.cachedaudioprovider.validation.ValidSongRequest;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ValidSongRequest
public class SongRequest {
    private String youtubeId;
    private String spotifyId;
    private String search;
    private String title;
    private String artist;
}

