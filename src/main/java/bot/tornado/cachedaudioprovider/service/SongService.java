package bot.tornado.cachedaudioprovider.service;

import bot.tornado.cachedaudioprovider.dto.SongMetadata;
import bot.tornado.cachedaudioprovider.dto.SongRequest;
import bot.tornado.cachedaudioprovider.exception.SongNotResolvableException;
import bot.tornado.cachedaudioprovider.model.Song;
import bot.tornado.cachedaudioprovider.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class SongService {
    private final SongRepository songRepository;
    private final YtDlpService ytDlpService;

    public enum SongRequestStatus {
        CACHED,
        ENQUEUED,
        INVALID
    }

    public SongRequestStatus getSongStatus(SongRequest request) {
        if (!request.getYoutubeId().isBlank()) {
            return this.getSongStatusByYoutubeId(request.getYoutubeId());
        } // TODO: Implement search, spotify, etc...
        throw new RuntimeException("Failed to get status for " + request);
    }

    // TODO: Implement search, spotify, etc...

    public SongRequestStatus getSongStatusByYoutubeId(String youtubeId) {
        Optional<Song> song = this.songRepository.findByYoutubeId(youtubeId);
        if (song.isPresent() && song.get().isCached()) {
            return SongRequestStatus.CACHED;
        }
        // TODO: add queue
        return SongRequestStatus.ENQUEUED;
    }

    public Song getOrDownloadSong(SongRequest request) throws SongNotResolvableException {
        if (!request.getYoutubeId().isBlank()) {
            return this.extractByYoutubeId(request.getYoutubeId());
        }
        throw new RuntimeException("Failed to extract song from request.");
    }

    private Song extractByYoutubeId(String youtubeId) {
        Song song = this.songRepository.findByYoutubeId(youtubeId).orElse(Song.builder().youtubeId(youtubeId).build());
        if (song.isCached()) {
            return song;
        }

        CompletableFuture<SongMetadata> future = this.ytDlpService.extractByYoutubeIdAsync(youtubeId);
        SongMetadata metadata = future.join();
        updateSongByMetadata(song, metadata);
        song.setCached(true);
        this.songRepository.save(song);
        return song;
    }

    private static void updateSongByMetadata(Song song, SongMetadata metadata) {
        song.setTitle(metadata.getTitle());
        song.setArtist(metadata.getArtist());
        song.setChannelUrl(metadata.getChannelUrl());
        song.setLikeCount(metadata.getLikeCount());
        song.setViewCount(metadata.getViewCount());
        song.setUploadDate(metadata.getUploadDate());
        song.setDuration(metadata.getDuration());
    }
}
