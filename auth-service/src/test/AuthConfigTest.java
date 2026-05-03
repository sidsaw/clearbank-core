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
    void setSessionTtlMsUpdatesValue() {
        config.setSessionTtlMs(7200_000L);
        assertEquals(7200_000L, config.getSessionTtlMs());
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
    void settingSessionTtlDoesNotAffectOtherFields() {
        int defaultMaxAttempts = config.getMaxLoginAttempts();
        long defaultLockoutDuration = config.getLockoutDurationMs();
        int defaultRateLimit = config.getRateLimitPerMinute();
        int defaultMinPassword = config.getMinPasswordLength();

        config.setSessionTtlMs(123_456L);

        assertEquals(defaultMaxAttempts, config.getMaxLoginAttempts());
        assertEquals(defaultLockoutDuration, config.getLockoutDurationMs());
        assertEquals(defaultRateLimit, config.getRateLimitPerMinute());
        assertEquals(defaultMinPassword, config.getMinPasswordLength());
    }

    @Test
    void settingMaxLoginAttemptsDoesNotAffectOtherFields() {
        long defaultSessionTtl = config.getSessionTtlMs();
        long defaultLockoutDuration = config.getLockoutDurationMs();
        int defaultRateLimit = config.getRateLimitPerMinute();
        int defaultMinPassword = config.getMinPasswordLength();

        config.setMaxLoginAttempts(99);

        assertEquals(defaultSessionTtl, config.getSessionTtlMs());
        assertEquals(defaultLockoutDuration, config.getLockoutDurationMs());
        assertEquals(defaultRateLimit, config.getRateLimitPerMinute());
        assertEquals(defaultMinPassword, config.getMinPasswordLength());
    }

    @Test
    void settingLockoutDurationDoesNotAffectOtherFields() {
        long defaultSessionTtl = config.getSessionTtlMs();
        int defaultMaxAttempts = config.getMaxLoginAttempts();
        int defaultRateLimit = config.getRateLimitPerMinute();
        int defaultMinPassword = config.getMinPasswordLength();

        config.setLockoutDurationMs(42L);

        assertEquals(defaultSessionTtl, config.getSessionTtlMs());
        assertEquals(defaultMaxAttempts, config.getMaxLoginAttempts());
        assertEquals(defaultRateLimit, config.getRateLimitPerMinute());
        assertEquals(defaultMinPassword, config.getMinPasswordLength());
    }

    @Test
    void settingRateLimitDoesNotAffectOtherFields() {
        long defaultSessionTtl = config.getSessionTtlMs();
        int defaultMaxAttempts = config.getMaxLoginAttempts();
        long defaultLockoutDuration = config.getLockoutDurationMs();
        int defaultMinPassword = config.getMinPasswordLength();

        config.setRateLimitPerMinute(7);

        assertEquals(defaultSessionTtl, config.getSessionTtlMs());
        assertEquals(defaultMaxAttempts, config.getMaxLoginAttempts());
        assertEquals(defaultLockoutDuration, config.getLockoutDurationMs());
        assertEquals(defaultMinPassword, config.getMinPasswordLength());
    }

    @Test
    void settingMinPasswordLengthDoesNotAffectOtherFields() {
        long defaultSessionTtl = config.getSessionTtlMs();
        int defaultMaxAttempts = config.getMaxLoginAttempts();
        long defaultLockoutDuration = config.getLockoutDurationMs();
        int defaultRateLimit = config.getRateLimitPerMinute();

        config.setMinPasswordLength(32);

        assertEquals(defaultSessionTtl, config.getSessionTtlMs());
        assertEquals(defaultMaxAttempts, config.getMaxLoginAttempts());
        assertEquals(defaultLockoutDuration, config.getLockoutDurationMs());
        assertEquals(defaultRateLimit, config.getRateLimitPerMinute());
    }

    @Test
    void multipleConfigsAreIndependent() {
        AuthConfig other = new AuthConfig();
        config.setSessionTtlMs(1L);
        config.setMaxLoginAttempts(1);
        assertEquals(3600_000L, other.getSessionTtlMs());
        assertEquals(5, other.getMaxLoginAttempts());
    }
}
