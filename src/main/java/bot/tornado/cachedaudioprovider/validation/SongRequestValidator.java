package bot.tornado.cachedaudioprovider.validation;

import bot.tornado.cachedaudioprovider.dto.SongRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SongRequestValidator implements ConstraintValidator<ValidSongRequest, SongRequest> {
    @Override
    public boolean isValid(SongRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return false;
        }
        return any(
            !isBlank(request.getYoutubeId()),
            !isBlank(request.getSpotifyId()),
            !(isBlank(request.getTitle()) && isBlank(request.getArtist()))
        );
    }

    private static boolean isBlank(String string) {
        return string == null || string.isBlank();
    }

    private static boolean any(Boolean... booleans) {
        for (Boolean bool : booleans) {
            if (bool) return true;
        }
        return false;
    }
}
