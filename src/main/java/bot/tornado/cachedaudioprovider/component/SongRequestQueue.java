package bot.tornado.cachedaudioprovider.component;

import bot.tornado.cachedaudioprovider.dto.SongRequest;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class SongRequestQueue {
    private final BlockingQueue<SongRequest> queue = new LinkedBlockingQueue<>(200);
    private final BlockingQueue<SongRequest> prioritizedQueue = new LinkedBlockingQueue<>(200);

    public boolean enqueue(final SongRequest request) {
        return switch (request.getPriority()) {
            case NORMAL -> this.queue.offer(request);
            case HIGH -> this.prioritizedQueue.offer(request);
        };
    }

    public SongRequest dequeue() {
        SongRequest request = this.prioritizedQueue.poll();
        if (request != null) {
            return request;
        }
        return this.queue.poll();
    }

    public int size() {
        return this.prioritizedQueue.size() + this.queue.size();
    }

    public boolean isEmpty() {
        return this.queue.isEmpty() && this.prioritizedQueue.isEmpty();
    }

    public boolean contains(final SongRequest request) {
        return this.prioritizedQueue.contains(request);
    }
}
