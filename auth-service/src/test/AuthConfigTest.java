import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthConfigTest {

    private AuthConfig config;

    @BeforeEach
    void setUp() {
        config = new AuthConfig();
    }

    @Test
    void defaultSessionTtlIsOneHour() {
        assertEquals(3_600_000L, config.getSessionTtlMs());
    }

    @Test
    void defaultMaxLoginAttemptsIsFive() {
        assertEquals(5, config.getMaxLoginAttempts());
    }

    @Test
    void defaultLockoutDurationIsFifteenMinutes() {
        assertEquals(900_000L, config.getLockoutDurationMs());
    }

    @Test
    void defaultRateLimitIsTwentyPerMinute() {
        assertEquals(20, config.getRateLimitPerMinute());
    }

    @Test
    void defaultMinPasswordLengthIsEight() {
        assertEquals(8, config.getMinPasswordLength());
    }

    @Test
    void setSessionTtlIsRoundTripped() {
        config.setSessionTtlMs(7_200_000L);
        assertEquals(7_200_000L, config.getSessionTtlMs());
    }

    @Test
    void setMaxLoginAttemptsIsRoundTripped() {
        config.setMaxLoginAttempts(10);
        assertEquals(10, config.getMaxLoginAttempts());
    }

    @Test
    void setLockoutDurationIsRoundTripped() {
        config.setLockoutDurationMs(60_000L);
        assertEquals(60_000L, config.getLockoutDurationMs());
    }

    @Test
    void setRateLimitIsRoundTripped() {
        config.setRateLimitPerMinute(100);
        assertEquals(100, config.getRateLimitPerMinute());
    }

    @Test
    void setMinPasswordLengthIsRoundTripped() {
        config.setMinPasswordLength(12);
        assertEquals(12, config.getMinPasswordLength());
    }

    @Test
    void zeroValuesAreAccepted() {
        // No validation is performed at the config layer; downstream components
        // are responsible for rejecting nonsensical values. This test pins the
        // current behaviour so a future addition of validation is a deliberate
        // change.
        config.setSessionTtlMs(0L);
        config.setMaxLoginAttempts(0);
        config.setLockoutDurationMs(0L);
        config.setRateLimitPerMinute(0);
        config.setMinPasswordLength(0);

        assertEquals(0L, config.getSessionTtlMs());
        assertEquals(0, config.getMaxLoginAttempts());
        assertEquals(0L, config.getLockoutDurationMs());
        assertEquals(0, config.getRateLimitPerMinute());
        assertEquals(0, config.getMinPasswordLength());
    }

    @Test
    void settersDoNotAffectOtherFields() {
        long originalSessionTtl = config.getSessionTtlMs();
        int originalMaxAttempts = config.getMaxLoginAttempts();
        long originalLockout = config.getLockoutDurationMs();
        int originalRateLimit = config.getRateLimitPerMinute();
        int originalPwLen = config.getMinPasswordLength();

        config.setSessionTtlMs(originalSessionTtl + 1);
        assertEquals(originalMaxAttempts, config.getMaxLoginAttempts());
        assertEquals(originalLockout, config.getLockoutDurationMs());
        assertEquals(originalRateLimit, config.getRateLimitPerMinute());
        assertEquals(originalPwLen, config.getMinPasswordLength());

        config.setMaxLoginAttempts(originalMaxAttempts + 1);
        assertEquals(originalSessionTtl + 1, config.getSessionTtlMs());
        assertEquals(originalLockout, config.getLockoutDurationMs());
        assertEquals(originalRateLimit, config.getRateLimitPerMinute());
        assertEquals(originalPwLen, config.getMinPasswordLength());

        config.setLockoutDurationMs(originalLockout + 1);
        assertEquals(originalSessionTtl + 1, config.getSessionTtlMs());
        assertEquals(originalMaxAttempts + 1, config.getMaxLoginAttempts());
        assertEquals(originalRateLimit, config.getRateLimitPerMinute());
        assertEquals(originalPwLen, config.getMinPasswordLength());

        config.setRateLimitPerMinute(originalRateLimit + 1);
        assertEquals(originalSessionTtl + 1, config.getSessionTtlMs());
        assertEquals(originalMaxAttempts + 1, config.getMaxLoginAttempts());
        assertEquals(originalLockout + 1, config.getLockoutDurationMs());
        assertEquals(originalPwLen, config.getMinPasswordLength());

        config.setMinPasswordLength(originalPwLen + 1);
        assertEquals(originalSessionTtl + 1, config.getSessionTtlMs());
        assertEquals(originalMaxAttempts + 1, config.getMaxLoginAttempts());
        assertEquals(originalLockout + 1, config.getLockoutDurationMs());
        assertEquals(originalRateLimit + 1, config.getRateLimitPerMinute());
    }

    @Test
    void independentInstancesDoNotShareState() {
        AuthConfig other = new AuthConfig();
        config.setSessionTtlMs(123L);
        config.setMaxLoginAttempts(99);
        assertEquals(3_600_000L, other.getSessionTtlMs());
        assertEquals(5, other.getMaxLoginAttempts());
    }
}
