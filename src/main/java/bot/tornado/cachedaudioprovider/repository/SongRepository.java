package bot.tornado.cachedaudioprovider.repository;

import bot.tornado.cachedaudioprovider.model.Song;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SongRepository extends JpaRepository<Song, String> {
    Optional<Song> findByYoutubeId(String youtubeId);

    Optional<Song> findBySpotifyId(String spotifyId);
}
