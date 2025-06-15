package bot.tornado.cachedaudioprovider.component;

import bot.tornado.cachedaudioprovider.dto.SongRequest;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;

@Component
public class SongRequestQueue {
    private final BlockingQueue<SongRequest> queue = new PriorityBlockingQueue<>(200,
            Comparator.comparing(SongRequest::getRequiredAt)
    );

    public boolean enqueue(final SongRequest request) {
        return this.queue.offer(request);
    }

    public SongRequest dequeue() {
        return this.queue.poll();
    }

    public int size() {
        return this.queue.size();
    }

    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    public boolean contains(final SongRequest request) {
        return this.queue.contains(request);
    }
}
