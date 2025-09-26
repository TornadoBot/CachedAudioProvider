package bot.tornado.cachedaudioprovider.validation;

import bot.tornado.cachedaudioprovider.dto.SongRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

public class SongRequestValidator implements ConstraintValidator<ValidSongRequest, SongRequest> {
    @Override
    public boolean isValid(SongRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return false;
        }
        return any(
            StringUtils.hasText(request.getYoutubeId()),
            StringUtils.hasText(request.getSearch()),
            (StringUtils.hasText(request.getTitle()) && StringUtils.hasText(request.getArtist()))
        );
    }

    private static boolean any(Boolean... booleans) {
        for (Boolean bool : booleans) {
            if (bool) return true;
        }
        return false;
    }
}
