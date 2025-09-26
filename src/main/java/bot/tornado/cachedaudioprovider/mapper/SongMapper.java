package bot.tornado.cachedaudioprovider.mapper;

import bot.tornado.cachedaudioprovider.dto.YtDlpEntry;
import bot.tornado.cachedaudioprovider.model.Song;
import lombok.experimental.UtilityClass;

import java.time.Instant;

/**
 * Utility class for mapping YtDlpEntry DTOs to Song entities.
 */
@UtilityClass
public class SongMapper {

    /**
     * Converts a {@link YtDlpEntry} into a new {@link Song} instance.
     *
     * @param entry the yt-dlp parsed entry
     * @return a fully initialized {@link Song} entity
     */
    public Song fromYtDlpEntry(YtDlpEntry entry) {
        return Song.builder()
                .youtubeId(entry.getId())
                .title(entry.getTitle())
                .artist(entry.getArtist())
                .duration(entry.getDuration())
                .likeCount(entry.getLikeCount())
                .viewCount(entry.getViewCount())
                .channelUrl(entry.getChannelUrl())
                .uploadDate(Instant.ofEpochSecond(entry.getUploadTimestamp()))
                .build();
    }

    /**
     * Updates the provided {@link Song} instance with values from the given {@link YtDlpEntry}.
     *
     * @param song  the song to update (typically already persisted)
     * @param entry the source data from yt-dlp
     */
    public void updateFromYtDlpEntry(Song song, YtDlpEntry entry) {
        song.setTitle(entry.getTitle());
        song.setArtist(entry.getArtist());
        song.setDuration(entry.getDuration());
        song.setLikeCount(entry.getLikeCount());
        song.setViewCount(entry.getViewCount());
        song.setChannelUrl(entry.getChannelUrl());
        song.setUploadDate(Instant.ofEpochSecond(entry.getUploadTimestamp()));
    }
}
