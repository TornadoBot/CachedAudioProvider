package bot.tornado.cachedaudioprovider.dto;

import bot.tornado.cachedaudioprovider.validation.ValidSongRequest;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@ValidSongRequest
public class SongRequest {
    private String youtubeId;
    private String search;
    private String title;
    private String artist;

    /**
     * Seconds until the requested source is required.
     */
    private Instant requiredAt;

    public enum Type {
        YOUTUBE_ID,
        SEARCH,
        TITLE_AND_ARTIST
    }

    public Type getType() {
        if (!this.youtubeId.isBlank()) {
            return Type.YOUTUBE_ID;
        }
        if (!this.search.isBlank()) {
            return Type.SEARCH;
        }
        return Type.TITLE_AND_ARTIST;
    }

    @Override
    public String toString() {
        return switch (this.getType()) {
            case YOUTUBE_ID -> this.youtubeId;
            case SEARCH -> this.search;
            case TITLE_AND_ARTIST -> "%s %s".formatted(this.title, this.artist);
        };
    }

    @Override
    public int hashCode() {
        return this.toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return this.hashCode() == o.hashCode();
    }
}

