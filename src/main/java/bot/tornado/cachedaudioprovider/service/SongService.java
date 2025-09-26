package bot.tornado.cachedaudioprovider.service;

import bot.tornado.cachedaudioprovider.component.SongRequestQueue;
import bot.tornado.cachedaudioprovider.component.SongRequestQueueWorker;
import bot.tornado.cachedaudioprovider.dto.SongRequest;
import bot.tornado.cachedaudioprovider.dto.SongResponse;
import bot.tornado.cachedaudioprovider.exception.SongNotResolvableException;
import bot.tornado.cachedaudioprovider.model.Song;
import bot.tornado.cachedaudioprovider.repository.SongRepository;
import bot.tornado.cachedaudioprovider.util.FuzzyMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SongService {
    private final SongRepository songRepository;
    private final SongRequestQueue queue;
    private final SongRequestQueueWorker worker;

    public enum SongRequestStatus {
        CACHED,
        ENQUEUED,
        PROCESSING,
        UNKNOWN
    }

    public SongRequestStatus getSongStatus(SongRequest request) {
        if (this.queue.contains(request)) {
            return SongRequestStatus.ENQUEUED;
        }
        if (Objects.equals(this.worker.getCurrent(), request)) {
            return SongRequestStatus.PROCESSING;
        }

        return switch (request.getType()) {
            case YOUTUBE_ID -> this.getSongStatusByYoutubeId(request.getYoutubeId());
            case SEARCH -> this.getSongStatusBySearch(request.getSearch());
            case TITLE_AND_ARTIST -> this.getSongStatusByTitleAndArtist(request.getTitle(), request.getArtist());
        };
    }

    public SongRequestStatus getSongStatusByYoutubeId(String youtubeId) {
        Optional<Song> song = this.songRepository.findByYoutubeId(youtubeId);
        if (song.isPresent() && song.get().isCached()) {
            return SongRequestStatus.CACHED;
        }
        return SongRequestStatus.UNKNOWN;
    }

    public SongRequestStatus getSongStatusBySearch(String search) {
        return this.getSongBySearch(search)
                .filter(Song::isCached)
                .map(song -> SongRequestStatus.CACHED)
                .orElse(SongRequestStatus.UNKNOWN);
    }

    public SongRequestStatus getSongStatusByTitleAndArtist(String title, String artist) {
        return this.getSongByTitleAndArtist(title, artist)
                .filter(Song::isCached)
                .map(song -> SongRequestStatus.CACHED)
                .orElse(SongRequestStatus.UNKNOWN);
    }

    public SongResponse getSong(SongRequest request) {
        Optional<Song> song = switch (request.getType()) {
            case YOUTUBE_ID -> this.getSongByYoutubeId(request.getYoutubeId());
            case SEARCH -> this.getSongBySearch(request.getSearch());
            case TITLE_AND_ARTIST -> this.getSongByTitleAndArtist(request.getTitle(), request.getArtist());
        };
        if (song.isPresent()) {
            return SongResponse.fromSong(song.get());
        }
        throw new SongNotResolvableException("No data is present for %s".formatted(request));
    }

    private Optional<Song> getSongByYoutubeId(String youtubeId) {
        return this.songRepository.findByYoutubeId(youtubeId);
    }

    private Optional<Song> getSongBySearch(String search) {
        List<Song> cachedSongs = this.songRepository.findAllByCachedTrue();

        return cachedSongs.stream()
                .map(song -> {
                    double titleScore = FuzzyMatch.similarity(song.getTitle(), search);
                    double combinedScore = FuzzyMatch.similarity("%s %s".formatted(song.getTitle(), song.getArtist()), search);
                    double similarity = Math.max(titleScore, combinedScore);
                    return new FuzzyMatch.Match<>(song, similarity);
                })
                .filter(match -> match.similarity() >= 0.6)
                .max(Comparator.comparing(FuzzyMatch.Match::similarity))
                .map(FuzzyMatch.Match::entry);
    }

    private Optional<Song> getSongByTitleAndArtist(String title, String artist) {
        List<Song> cachedSongs = this.songRepository.findAllByCachedTrue();

        return cachedSongs.stream()
                .map(song -> {
                    double titleScore = FuzzyMatch.similarity(song.getTitle(), title);
                    double artistScore = FuzzyMatch.similarity(song.getArtist(), artist);
                    double similarity = (titleScore + artistScore) / 2.0;
                    return new FuzzyMatch.Match<>(song, similarity);
                })
                .filter(match -> match.similarity() >= 0.7)
                .max(Comparator.comparing(FuzzyMatch.Match::similarity))
                .map(FuzzyMatch.Match::entry);
    }
}
