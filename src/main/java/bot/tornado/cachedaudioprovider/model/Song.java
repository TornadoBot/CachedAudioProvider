package bot.tornado.cachedaudioprovider.model;

import bot.tornado.cachedaudioprovider.dto.SongMetadata;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.boot.Metadata;

import java.time.Instant;

@Entity
@Table(name = "songs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Song {

    @Id
    private String youtubeId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String artist;

    private String channelUrl;

    private Integer likeCount;

    private Integer viewCount;

    private Instant uploadDate;

    private Integer duration; // seconds

    private boolean cached;

    @Column(unique = true)
    private String spotifyId;

    @Column(nullable = false)
    private Instant extractedAt;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        this.extractedAt = Instant.now();
    }

    public void updateByMetadata(final SongMetadata metadata) {
        this.title = metadata.getTitle();
        this.artist = metadata.getArtist();
        this.channelUrl = metadata.getChannelUrl();
        this.likeCount = metadata.getLikeCount();
        this.viewCount = metadata.getViewCount();
        this.uploadDate = metadata.getUploadDate();
        this.duration = metadata.getDuration();
    }
}
