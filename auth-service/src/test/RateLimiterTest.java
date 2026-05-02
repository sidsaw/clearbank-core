import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterTest {

    private RateLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new RateLimiter(3);
    }

    @Test
    void firstRequestIsAllowed() {
        assertDoesNotThrow(() -> limiter.checkRate("ip1"));
    }

    @Test
    void requestsWithinLimitAreAllowed() {
        limiter.checkRate("ip1");
        limiter.checkRate("ip1");
        limiter.checkRate("ip1");
        // 3 requests should be fine (limit is 3)
    }

    @Test
    void requestExceedingLimitThrows() {
        limiter.checkRate("ip1");
        limiter.checkRate("ip1");
        limiter.checkRate("ip1");
        assertThrows(IllegalStateException.class, () -> limiter.checkRate("ip1"));
    }

    @Test
    void differentKeysHaveSeparateLimits() {
        limiter.checkRate("ip1");
        limiter.checkRate("ip1");
        limiter.checkRate("ip1");
        assertDoesNotThrow(() -> limiter.checkRate("ip2"));
    }

    @Test
    void isRateLimitedReturnsFalseInitially() {
        assertFalse(limiter.isRateLimited("ip1"));
    }

    @Test
    void resetClearsCounterForKey() {
        limiter.checkRate("ip1");
        limiter.checkRate("ip1");
        limiter.checkRate("ip1");
        limiter.reset("ip1");
        assertDoesNotThrow(() -> limiter.checkRate("ip1"));
    }
}
