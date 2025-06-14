package bot.tornado.cachedaudioprovider.service;

import bot.tornado.cachedaudioprovider.dto.SongMetadata;
import bot.tornado.cachedaudioprovider.dto.SongRequest;
import bot.tornado.cachedaudioprovider.model.Song;
import bot.tornado.cachedaudioprovider.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class SongProcessor {
    private final SongRepository songRepository;
    private final YtDlpService ytDlpService;

    public void process(final SongRequest request) {
        switch (request.getType()) {
            case YOUTUBE_ID -> processByYoutubeId(request.getYoutubeId());
            case SPOTIFY_ID -> processBySpotifyId(request.getSpotifyId());
            case SEARCH -> processBySearch(request.getSearch());
            case TITLE_AND_ARTIST -> processByTitleAndArtist(request.getTitle(), request.getArtist());
        }
    }

    private void processByYoutubeId(final String youtubeId) {
        Song song = this.songRepository.findByYoutubeId(youtubeId)
                .orElse(Song.builder().youtubeId(youtubeId).build());
        if (song.isCached()) {
            return;
        }
        song.updateByMetadata(this.ytDlpService.extractByYoutubeId(youtubeId));
        song.setCached(true);
        this.songRepository.save(song);
    }

    private void processBySpotifyId(final String spotifyId) {
        // TODO: Implement.
        // Requires Spotify API Service
    }

    private void processBySearch(final String search) {
        // TODO: Implement.
    }

    private void processByTitleAndArtist(final String title, final String artist) { // TODO: Implement Fuzzy Match
        Optional<Song> existing = this.songRepository.findByTitleAndArtist(title, artist);
        if (existing.isPresent() && existing.get().isCached()) {
            return;
        }

        SongMetadata metadata = this.ytDlpService.extractByTitleAndArtist(title, artist);
        Song song =  new Song();
        song.setYoutubeId(metadata.getYoutubeId());
        song.updateByMetadata(metadata);
        song.setCached(true);

        this.songRepository.save(song);
    }
}
