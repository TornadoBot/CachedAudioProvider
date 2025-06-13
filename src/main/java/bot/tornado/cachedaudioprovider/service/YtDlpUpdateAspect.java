package bot.tornado.cachedaudioprovider.service;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class YtDlpUpdateAspect {

    private final YtDlpUpdater updater;

    @Before("@annotation(bot.tornado.cachedaudioprovider.aop.EnsureYtDlpUpdated)")
    public void ensureYtDlpUpdated() {
        this.updater.updateIfDue();
    }
}
