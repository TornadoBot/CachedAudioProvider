package bot.tornado.cachedaudioprovider.controller;

import bot.tornado.cachedaudioprovider.dto.SongRequest;
import bot.tornado.cachedaudioprovider.model.Song;
import bot.tornado.cachedaudioprovider.service.SongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController("/song")
@RequiredArgsConstructor
public class SongRequestController {
    private final SongService songService;

    public ResponseEntity<Void> songRequest(@Valid @RequestBody SongRequest request) {
        // TODO: Use queue
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus(@Valid @RequestBody SongRequest request) {
        SongService.SongRequestStatus status = this.songService.getSongStatus(request);
        return switch (status) {
            case CACHED -> ResponseEntity.ok().build();
            case ENQUEUED -> ResponseEntity.accepted().build();
            case INVALID -> ResponseEntity.notFound().build();
        };
    }
}
