package bot.tornado.cachedaudioprovider.service;

import bot.tornado.cachedaudioprovider.dto.SongMetadata;
import bot.tornado.cachedaudioprovider.dto.SongRequest;
import bot.tornado.cachedaudioprovider.exception.SongNotResolvableException;
import bot.tornado.cachedaudioprovider.model.Song;
import bot.tornado.cachedaudioprovider.repository.SongRepository;
import bot.tornado.cachedaudioprovider.util.FuzzyMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SongService {
    private final SongRepository songRepository;
    private final YtDlpService ytDlpService;

    public Song getOrDownloadSong(SongRequest request) throws SongNotResolvableException {
        SongMetadata metadata = null;

        if (request.getYoutubeId() != null) {
            Optional<Song> song = this.songRepository.findByYoutubeId(request.getYoutubeId());
            if (song.isPresent()) {
                return song.get();
            }
            metadata = this.ytDlpService.extractByYoutubeId(request.getYoutubeId());
        }
        if (request.getSpotifyId() != null) {
            Optional<Song> song = this.songRepository.findBySpotifyId(request.getSpotifyId());
            if (song.isPresent()) {
                return song.get();
            }
            // TODO: ADD SUPPORT FOR SPOTIFY
        }
        if (request.getSearch() != null) {
            FuzzyMatch.Tuple<Song> match = null;
            try {
                match = FuzzyMatch.search(
                        this.songRepository.findAll(),
                        request.getSearch()
                ).getFirst();
            } catch (NoSuchElementException ignored) {
                metadata = YtDlpService.extractBySearch(request.getSearch());
            }

            if (match != null) {
                double similarity = FuzzyMatch.getSimilarity(
                        match,
                        request.getSearch()
                );
                if (similarity > 0.75) {
                    return match.entry();
                }
                metadata = YtDlpService.extractBySearch(request.getSearch());
            }
        }

        // TODO: IMPLEMENT ADVANCED SEARCH

        if (metadata == null) {
            throw new SongNotResolvableException();
        }
        Song song = buildFromMetadata(metadata, request);
        this.songRepository.save(song);
        return song;
    }

    public void deleteSong(String youtubeId) {
        this.songRepository.deleteById(youtubeId);
    }

    private Song buildFromMetadata(SongMetadata metadata, SongRequest request) {
        return Song
                .builder()
                .youtubeId(metadata.getYoutubeId())
                .title(metadata.getTitle())
                .artist(metadata.getArtist())
                .channelUrl(metadata.getChannelUrl())
                .likeCount(metadata.getLikeCount())
                .viewCount(metadata.getViewCount())
                .uploadDate(metadata.getUploadDate())
                .duration(metadata.getDuration())
                .spotifyId(request.getSpotifyId())
                .build();
    }
}
