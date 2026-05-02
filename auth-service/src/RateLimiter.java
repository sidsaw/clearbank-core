import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RateLimiter {

    private final int maxRequestsPerMinute;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequestsPerMinute) {
        this.maxRequestsPerMinute = maxRequestsPerMinute;
    }

    public void checkRate(String key) {
        WindowCounter counter = counters.computeIfAbsent(key, k -> new WindowCounter());
        long now = System.currentTimeMillis();

        if (now - counter.windowStart > 60_000) {
            counter.reset(now);
        }

        if (counter.count.incrementAndGet() > maxRequestsPerMinute) {
            throw new IllegalStateException("Rate limit exceeded for: " + key);
        }
    }

    public boolean isRateLimited(String key) {
        WindowCounter counter = counters.get(key);
        if (counter == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now - counter.windowStart > 60_000) {
            return false;
        }
        return counter.count.get() >= maxRequestsPerMinute;
    }

    public void reset(String key) {
        counters.remove(key);
    }

    private static class WindowCounter {
        volatile long windowStart;
        final AtomicInteger count = new AtomicInteger(0);

        WindowCounter() {
            this.windowStart = System.currentTimeMillis();
        }

        void reset(long now) {
            this.windowStart = now;
            this.count.set(0);
        }
    }
}
