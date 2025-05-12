package bot.tornado.cachedaudioprovider.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler
    public ResponseEntity<String> handleSongNotResolvable(SongNotResolvableException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
