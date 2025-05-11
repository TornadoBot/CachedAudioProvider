package bot.tornado.cachedaudioprovider.exception;

public class SongNotResolvableException extends RuntimeException {
    public SongNotResolvableException() {
        super("Could not resolve a song from the given request.");
    }

    public SongNotResolvableException(String message) {
        super(message);
    }

    public SongNotResolvableException(String message, Throwable cause) {
        super(message, cause);
    }
}
