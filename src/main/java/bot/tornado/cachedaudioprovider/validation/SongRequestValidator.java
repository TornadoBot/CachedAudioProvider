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
        return isNotBlank(request.getYoutubeId())
                || isNotBlank(request.getSpotifyId())
                || isNotBlank(request.getSearch())
                || (isNotBlank(request.getTitle()) && isNotBlank(request.getArtist()));
    }

    private boolean isNotBlank(String string) {
        return string != null && !string.isEmpty();
    }
}
