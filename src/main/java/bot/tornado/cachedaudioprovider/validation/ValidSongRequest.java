package bot.tornado.cachedaudioprovider.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SongRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSongRequest {
    String message() default "At least one identifier must be provided.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
