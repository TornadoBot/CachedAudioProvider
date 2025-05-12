package bot.tornado.cachedaudioprovider.model;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(unique = true)
    private String spotifyId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
