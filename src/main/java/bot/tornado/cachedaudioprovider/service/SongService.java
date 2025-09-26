package bot.tornado.cachedaudioprovider.service;

import bot.tornado.cachedaudioprovider.component.SongRequestQueue;
import bot.tornado.cachedaudioprovider.component.SongRequestQueueWorker;
import bot.tornado.cachedaudioprovider.dto.SongRequest;
import bot.tornado.cachedaudioprovider.model.Song;
import bot.tornado.cachedaudioprovider.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
        if (this.worker.getCurrent() == request) {
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

    public SongRequestStatus getSongStatusBySearch(String ignored) {
        // TODO: Implement search
        return null;
    }

    public SongRequestStatus getSongStatusByTitleAndArtist(String title, String artist) {
        Optional<Song> song = this.songRepository.findByTitleAndArtist(title, artist); // TODO: Implement closed match
        if (song.isPresent() && song.get().isCached()) {
            return SongRequestStatus.CACHED;
        }
        return SongRequestStatus.UNKNOWN;
    }
}
