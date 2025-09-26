package bot.tornado.cachedaudioprovider.service;

import bot.tornado.cachedaudioprovider.dto.SongRequest;
import bot.tornado.cachedaudioprovider.dto.YtDlpEntry;
import bot.tornado.cachedaudioprovider.exception.SongNotResolvableException;
import bot.tornado.cachedaudioprovider.mapper.SongMapper;
import bot.tornado.cachedaudioprovider.model.Song;
import bot.tornado.cachedaudioprovider.repository.SongRepository;
import bot.tornado.cachedaudioprovider.service.extraction.YtDlpService;
import bot.tornado.cachedaudioprovider.util.FuzzyMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Service responsible for resolving and processing song requests.
 * <p>
 * This class provides functionality to:
 * <ul>
 *     <li>Retrieve a song by its YouTube ID</li>
 *     <li>Find and match a song by a fuzzy search query</li>
 *     <li>Match a song by exact title and artist</li>
 * </ul>
 * <p>
 * The class uses a local {@link SongRepository} as cache. If no suitable match is found,
 * it performs metadata extraction using {@link YtDlpService} and stores the result in the database.
 * <p>
 * Mapping between raw yt-dlp output and {@link Song} entities is done through the {@link SongMapper}.
 */
@Service
@RequiredArgsConstructor
public class SongProcessor {
    private final YtDlpService ytDlpService;
    private final SongRepository songRepository;

    /**
     * Dispatches a {@link SongRequest} to the appropriate processing strategy.
     *
     * <p>This method acts as an entry point for handling song resolution requests.
     * Depending on the available fields in the request, it chooses one of the following:
     * <ul>
     *   <li>{@code youtubeId} → resolves directly via {@link #processFromYoutubeId(String)}</li>
     *   <li>{@code search} → resolves via free text search using {@link #processFromSearch(String)}</li>
     *   <li>{@code title + artist} → resolves via combined title/artist search using {@link #processFromTitleAndArtist(String, String)}</li>
     * </ul>
     *
     * <p>If none of the required fields are present, the request is considered invalid
     * and a {@link SongNotResolvableException} will be thrown.
     *
     * @param songRequest the {@link SongRequest} containing resolution parameters
     * @return the resolved and persisted {@link Song}
     * @throws SongNotResolvableException if the request does not contain enough information to resolve a song
     */
    @Transactional
    public void process(SongRequest songRequest) throws SongNotResolvableException {

        if (StringUtils.hasText(songRequest.getYoutubeId())) {
            this.processFromYoutubeId(songRequest.getYoutubeId());
        }

        if (StringUtils.hasText(songRequest.getSearch())) {
            this.processFromSearch(songRequest.getSearch());
        }

        if (StringUtils.hasText(songRequest.getTitle()) && StringUtils.hasText(songRequest.getArtist())) {
            this.processFromTitleAndArtist(
                    songRequest.getTitle(),
                    songRequest.getArtist()
            );
        }
    }


    /**
     * Resolves a song using a YouTube video ID.
     * <p>
     * If a song with the given ID already exists and is cached, it is returned directly.
     * Otherwise, yt-dlp is invoked to fetch metadata, which is then saved and returned.
     *
     * @param youtubeId the YouTube video ID
     * @return the resolved and persisted {@link Song}
     * @throws SongNotResolvableException if extraction fails
     */
    private Song processFromYoutubeId(String youtubeId) {
        String url = "https://www.youtube.com/watch?v=%s".formatted(youtubeId);

        return this.songRepository.findByYoutubeId(youtubeId)
                .map(song -> {
                    if (song.isCached()) return song;
                    YtDlpEntry entry = this.ytDlpService.extractByUrl(url)
                            .orElseThrow(SongNotResolvableException::new);
                    SongMapper.updateFromYtDlpEntry(song, entry);
                    song.setCached(true);
                    return this.songRepository.save(song);
                })
                .orElseGet(() -> {
                    YtDlpEntry entry = this.ytDlpService.extractByUrl(url)
                            .orElseThrow(SongNotResolvableException::new);
                    Song song = SongMapper.fromYtDlpEntry(entry);
                    song.setCached(true);
                    return this.songRepository.save(song);
                });
    }

    /**
     * Resolves a song from a free-form search string.
     * <p>
     * Attempts to match the search against cached songs using fuzzy title/artist comparisons.
     * If no sufficient match is found, yt-dlp is used to search YouTube Music and extract metadata.
     *
     * @param search the user-provided query (e.g. "the cure pictures of you")
     * @return a resolved {@link Song}, either from cache or extracted
     * @throws SongNotResolvableException if no suitable result is found
     */
    private Song processFromSearch(String search) {
        List<Song> cachedSongs = this.songRepository.findAllByCachedTrue();

        Optional<FuzzyMatch.Match<Song>> bestMatch = Stream.of(
                        FuzzyMatch.bestMatch(cachedSongs, search, Song::getTitle, 0.7),
                        FuzzyMatch.bestMatch(cachedSongs, search,
                                s -> "%s %s".formatted(s.getTitle(), s.getArtist()), 0.6)
                ).filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparing(FuzzyMatch.Match::similarity));

        if (bestMatch.isPresent()) {
            return bestMatch.get().entry();
        }

        List<YtDlpEntry> results = this.ytDlpService.extractBySearchQuery(search);
        if (results.isEmpty()) {
            throw new SongNotResolvableException();
        }

        YtDlpEntry entry = FuzzyMatch.bestMatch(
                results, search,
                (YtDlpEntry entry1) -> "%s %s".formatted(entry1.getTitle(), entry1.getArtist()),
                0
        ).orElseThrow(SongNotResolvableException::new).entry();
        return this.processFromYoutubeId(entry.getId());
    }

    /**
     * Resolves a song from a known title and artist.
     * <p>
     * First attempts a fuzzy match in the cached song list by comparing title and artist separately.
     * If no cached match is found, yt-dlp is used to search YouTube Music with both parameters combined.
     *
     * @param title  the song title (e.g. "Bohemian Rhapsody")
     * @param artist the song artist (e.g. "Queen")
     * @return a resolved {@link Song}, either from cache or freshly extracted
     * @throws SongNotResolvableException if no matching result is found
     */
    private Song processFromTitleAndArtist(String title, String artist) {
        List<Song> cachedSongs = this.songRepository.findAllByCachedTrue();

        Optional<FuzzyMatch.Match<Song>> match = cachedSongs.stream()
                .map(song -> {
                    double titleScore = FuzzyMatch.similarity(song.getTitle(), title);
                    double artistScore = FuzzyMatch.similarity(song.getArtist(), artist);
                    double combinedScore = (titleScore + artistScore) / 2.0;
                    return new FuzzyMatch.Match<>(song, combinedScore);
                })
                .filter(m -> m.similarity() >= 0.7)
                .max(Comparator.comparing(FuzzyMatch.Match::similarity));

        if (match.isPresent()) {
            return match.get().entry();
        }

        String search = "%s %s".formatted(title, artist);
        List<YtDlpEntry> results = this.ytDlpService.extractBySearchQuery(search);
        if (results.isEmpty()) {
            throw new SongNotResolvableException();
        }

        String bestId = FuzzyMatch.bestMatch(
                results,
                search,
                e -> "%s %s".formatted(e.getTitle(), e.getArtist()),
                0
        ).orElseThrow(SongNotResolvableException::new).entry().getId();
        return this.processFromYoutubeId(bestId);
    }
}
