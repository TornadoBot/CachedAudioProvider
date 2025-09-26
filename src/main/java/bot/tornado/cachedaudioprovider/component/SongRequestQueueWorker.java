package bot.tornado.cachedaudioprovider.component;

import bot.tornado.cachedaudioprovider.dto.SongRequest;
import bot.tornado.cachedaudioprovider.service.SongProcessor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SongRequestQueueWorker {
    private final SongRequestQueue queue;
    private final SongProcessor processor;

    @Getter
    private SongRequest current;

    @Scheduled(fixedDelay = 2000)
    public void run() {
        this.current = this.queue.dequeue();
        if (this.current == null) {
            return;
        }
        try {
            log.info("Processing song request: {}", this.current);
            this.processor.process(this.current);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            this.current = null;
        }
    }
}
