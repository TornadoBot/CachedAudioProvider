package bot.tornado.cachedaudioprovider.controller;

import bot.tornado.cachedaudioprovider.dto.SongRequest;
import bot.tornado.cachedaudioprovider.component.SongRequestQueue;
import bot.tornado.cachedaudioprovider.service.SongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/songs")
@RequiredArgsConstructor
public class SongRequestController {
    private final SongService songService;
    private final SongRequestQueue songRequestQueue;

    @PostMapping("/request")
    public ResponseEntity<Void> songRequest(@Valid @RequestBody SongRequest request) {
        if (this.songRequestQueue.enqueue(request)) {
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.unprocessableEntity().build();
    }

    @GetMapping("/status")
    public ResponseEntity<Void> getStatus(@Valid @RequestBody SongRequest request) {
        SongService.SongRequestStatus status = this.songService.getSongStatus(request);
        return switch (status) {
            case CACHED -> ResponseEntity.ok().build();
            case ENQUEUED, PROCESSING -> ResponseEntity.accepted().build();
            case UNKNOWN -> ResponseEntity.notFound().build();
        };
    }
}
