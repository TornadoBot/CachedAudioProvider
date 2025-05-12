package bot.tornado.cachedaudioprovider.controller;

import bot.tornado.cachedaudioprovider.dto.SongRequest;
import bot.tornado.cachedaudioprovider.dto.SongResponse;
import bot.tornado.cachedaudioprovider.model.Song;
import bot.tornado.cachedaudioprovider.service.SongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/songs")
@RequiredArgsConstructor
public class SongController {
    private final SongService songService;

    @PostMapping
    public ResponseEntity<SongResponse> getOrDownloadSong(@Valid @RequestBody SongRequest request) {
        Song song = this.songService.getOrDownloadSong(request);
        SongResponse response = SongResponse.fromSong(song);
        return ResponseEntity.ok(response);
    }
}
