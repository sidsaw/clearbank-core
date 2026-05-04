import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class AccountLockoutTest {

    private AccountLockout lockout;

    @BeforeEach
    void setUp() {
        lockout = new AccountLockout(3, 60_000);
    }

    @Test
    void newAccountIsNotLocked() {
        assertFalse(lockout.isLocked("alice"));
    }

    @Test
    void initialFailureCountIsZero() {
        assertEquals(0, lockout.getFailureCount("alice"));
    }

    @Test
    void recordFailureIncrementsFailureCount() {
        lockout.recordFailure("alice");
        assertEquals(1, lockout.getFailureCount("alice"));
        lockout.recordFailure("alice");
        assertEquals(2, lockout.getFailureCount("alice"));
    }

    @Test
    void accountIsNotLockedBeforeReachingMaxAttempts() {
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        assertFalse(lockout.isLocked("alice"));
    }

    @Test
    void accountLocksAtMaxAttempts() {
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        assertTrue(lockout.isLocked("alice"));
    }

    @Test
    void accountStaysLockedBeyondMaxAttempts() {
        for (int i = 0; i < 5; i++) {
            lockout.recordFailure("alice");
        }
        assertTrue(lockout.isLocked("alice"));
    }

    @Test
    void resetFailuresClearsCount() {
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        lockout.resetFailures("alice");
        assertEquals(0, lockout.getFailureCount("alice"));
    }

    @Test
    void resetFailuresUnlocksLockedAccount() {
        AccountLockout twoStrike = new AccountLockout(2, 60_000);
        twoStrike.recordFailure("alice");
        twoStrike.recordFailure("alice");
        assertTrue(twoStrike.isLocked("alice"));

        twoStrike.resetFailures("alice");

        assertFalse(twoStrike.isLocked("alice"));
        assertEquals(0, twoStrike.getFailureCount("alice"));
    }

    @Test
    void resetFailuresOnUnknownUserIsNoOp() {
        assertDoesNotThrow(() -> lockout.resetFailures("ghost"));
        assertEquals(0, lockout.getFailureCount("ghost"));
        assertFalse(lockout.isLocked("ghost"));
    }

    @Test
    void differentUsersHaveIndependentLockoutState() {
        for (int i = 0; i < 3; i++) {
            lockout.recordFailure("alice");
        }

        assertTrue(lockout.isLocked("alice"));
        assertFalse(lockout.isLocked("bob"));
        assertEquals(0, lockout.getFailureCount("bob"));
    }

    @Test
    void lockoutExpiresAfterConfiguredDuration() throws InterruptedException {
        AccountLockout shortLock = new AccountLockout(2, 50);
        shortLock.recordFailure("alice");
        shortLock.recordFailure("alice");
        assertTrue(shortLock.isLocked("alice"));

        Thread.sleep(120);

        assertFalse(shortLock.isLocked("alice"));
    }

    @Test
    void expiredLockoutResetsFailureCount() throws InterruptedException {
        AccountLockout shortLock = new AccountLockout(2, 50);
        shortLock.recordFailure("alice");
        shortLock.recordFailure("alice");
        assertEquals(2, shortLock.getFailureCount("alice"));

        Thread.sleep(120);

        // isLocked has the side effect of clearing expired state.
        assertFalse(shortLock.isLocked("alice"));
        assertEquals(0, shortLock.getFailureCount("alice"));
    }

    @Test
    void getFailureCountReflectsFullLifecycle() {
        assertEquals(0, lockout.getFailureCount("alice"));

        lockout.recordFailure("alice");
        assertEquals(1, lockout.getFailureCount("alice"));

        lockout.recordFailure("alice");
        assertEquals(2, lockout.getFailureCount("alice"));

        lockout.resetFailures("alice");
        assertEquals(0, lockout.getFailureCount("alice"));
    }

    @Test
    void concurrentAccessAcrossUsersIsThreadSafe() throws InterruptedException {
        int threads = 20;
        int failuresPerUser = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                final String user = "user-" + i;
                pool.submit(() -> {
                    for (int j = 0; j < failuresPerUser; j++) {
                        lockout.recordFailure(user);
                        lockout.isLocked(user);
                    }
                });
            }
        } finally {
            pool.shutdown();
            assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        }

        for (int i = 0; i < threads; i++) {
            assertEquals(failuresPerUser, lockout.getFailureCount("user-" + i));
        }
    }
}
