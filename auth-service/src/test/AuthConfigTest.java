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
        assertEquals(3600_000L, config.getSessionTtlMs());
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
    void setSessionTtlUpdatesGetter() {
        config.setSessionTtlMs(7200_000L);
        assertEquals(7200_000L, config.getSessionTtlMs());
    }

    @Test
    void setMaxLoginAttemptsUpdatesGetter() {
        config.setMaxLoginAttempts(10);
        assertEquals(10, config.getMaxLoginAttempts());
    }

    @Test
    void setLockoutDurationUpdatesGetter() {
        config.setLockoutDurationMs(120_000L);
        assertEquals(120_000L, config.getLockoutDurationMs());
    }

    @Test
    void setRateLimitPerMinuteUpdatesGetter() {
        config.setRateLimitPerMinute(60);
        assertEquals(60, config.getRateLimitPerMinute());
    }

    @Test
    void setMinPasswordLengthUpdatesGetter() {
        config.setMinPasswordLength(12);
        assertEquals(12, config.getMinPasswordLength());
    }

    @Test
    void zeroAndBoundaryValuesAreAccepted() {
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
    void changingOneFieldDoesNotAffectOthers() {
        long origSessionTtl = config.getSessionTtlMs();
        long origLockout = config.getLockoutDurationMs();
        int origRateLimit = config.getRateLimitPerMinute();
        int origMinPass = config.getMinPasswordLength();

        config.setMaxLoginAttempts(99);

        assertEquals(99, config.getMaxLoginAttempts());
        assertEquals(origSessionTtl, config.getSessionTtlMs());
        assertEquals(origLockout, config.getLockoutDurationMs());
        assertEquals(origRateLimit, config.getRateLimitPerMinute());
        assertEquals(origMinPass, config.getMinPasswordLength());
    }

    @Test
    void allFieldsAreIndependentlyMutable() {
        config.setSessionTtlMs(1L);
        config.setMaxLoginAttempts(2);
        config.setLockoutDurationMs(3L);
        config.setRateLimitPerMinute(4);
        config.setMinPasswordLength(5);

        assertEquals(1L, config.getSessionTtlMs());
        assertEquals(2, config.getMaxLoginAttempts());
        assertEquals(3L, config.getLockoutDurationMs());
        assertEquals(4, config.getRateLimitPerMinute());
        assertEquals(5, config.getMinPasswordLength());
    }

    @Test
    void freshInstancesHaveIndependentState() {
        AuthConfig first = new AuthConfig();
        AuthConfig second = new AuthConfig();
        first.setMaxLoginAttempts(99);
        assertEquals(5, second.getMaxLoginAttempts());
    }
}
