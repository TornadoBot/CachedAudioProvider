package bot.tornado.cachedaudioprovider.dto;

import bot.tornado.cachedaudioprovider.model.Song;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class SongResponse {
    private String youtubeId;
    private String title;
    private String artist;
    private Integer duration;
    private String streamUrl;
    private String channelUrl;
    private Integer likeCount;
    private Integer viewCount;
    private Instant uploadDate;

    public static SongResponse fromSong(Song song) {
        return SongResponse.builder()
                .youtubeId(song.getYoutubeId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .duration(song.getDuration())
                .streamUrl("/songs/stream/%s".formatted(song.getYoutubeId()))
                .channelUrl(song.getChannelUrl())
                .likeCount(song.getLikeCount())
                .viewCount(song.getViewCount())
                .uploadDate(song.getUploadDate())
                .build();
    }
}
