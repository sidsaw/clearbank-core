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
    void setSessionTtlMsUpdatesValue() {
        config.setSessionTtlMs(7_200_000L);
        assertEquals(7_200_000L, config.getSessionTtlMs());
    }

    @Test
    void setMaxLoginAttemptsUpdatesValue() {
        config.setMaxLoginAttempts(10);
        assertEquals(10, config.getMaxLoginAttempts());
    }

    @Test
    void setLockoutDurationMsUpdatesValue() {
        config.setLockoutDurationMs(60_000L);
        assertEquals(60_000L, config.getLockoutDurationMs());
    }

    @Test
    void setRateLimitPerMinuteUpdatesValue() {
        config.setRateLimitPerMinute(100);
        assertEquals(100, config.getRateLimitPerMinute());
    }

    @Test
    void setMinPasswordLengthUpdatesValue() {
        config.setMinPasswordLength(16);
        assertEquals(16, config.getMinPasswordLength());
    }

    @Test
    void settingOneFieldDoesNotAffectOthers() {
        config.setSessionTtlMs(1L);

        assertEquals(5, config.getMaxLoginAttempts());
        assertEquals(900_000L, config.getLockoutDurationMs());
        assertEquals(20, config.getRateLimitPerMinute());
        assertEquals(8, config.getMinPasswordLength());
    }

    @Test
    void allSettersAreIndependent() {
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
    void extremeValuesAreAccepted() {
        config.setSessionTtlMs(0L);
        config.setMaxLoginAttempts(0);
        config.setLockoutDurationMs(Long.MAX_VALUE);
        config.setRateLimitPerMinute(Integer.MAX_VALUE);
        config.setMinPasswordLength(Integer.MAX_VALUE);

        assertEquals(0L, config.getSessionTtlMs());
        assertEquals(0, config.getMaxLoginAttempts());
        assertEquals(Long.MAX_VALUE, config.getLockoutDurationMs());
        assertEquals(Integer.MAX_VALUE, config.getRateLimitPerMinute());
        assertEquals(Integer.MAX_VALUE, config.getMinPasswordLength());
    }

    @Test
    void independentInstancesDoNotShareState() {
        AuthConfig other = new AuthConfig();
        config.setSessionTtlMs(1L);
        config.setMaxLoginAttempts(99);

        assertEquals(3_600_000L, other.getSessionTtlMs());
        assertEquals(5, other.getMaxLoginAttempts());
    }
}
