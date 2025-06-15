package bot.tornado.cachedaudioprovider.service.spotify;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SpotifyTokenAspect {
    private final SpotifyApiService spotifyApiService;

    @Before("@annotation(bot.tornado.cachedaudioprovider.aop.EnsureSpotifyToken)")
    public void ensureToken() {
        this.spotifyApiService.ensureToken();
    }
}
