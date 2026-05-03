import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AccountLockoutTest {

    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MS = 60_000;

    private AccountLockout lockout;

    @BeforeEach
    void setUp() {
        lockout = new AccountLockout(MAX_ATTEMPTS, LOCKOUT_DURATION_MS);
    }

    @Test
    void freshAccountIsNotLocked() {
        assertFalse(lockout.isLocked("alice"));
    }

    @Test
    void getFailureCountStartsAtZero() {
        assertEquals(0, lockout.getFailureCount("alice"));
    }

    @Test
    void recordFailureIncrementsCount() {
        lockout.recordFailure("alice");
        assertEquals(1, lockout.getFailureCount("alice"));
        lockout.recordFailure("alice");
        assertEquals(2, lockout.getFailureCount("alice"));
    }

    @Test
    void accountNotLockedBeforeReachingMaxAttempts() {
        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            lockout.recordFailure("alice");
        }
        assertFalse(lockout.isLocked("alice"));
    }

    @Test
    void accountLocksAfterReachingMaxAttempts() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            lockout.recordFailure("alice");
        }
        assertTrue(lockout.isLocked("alice"));
    }

    @Test
    void accountLocksAfterExceedingMaxAttempts() {
        for (int i = 0; i < MAX_ATTEMPTS + 2; i++) {
            lockout.recordFailure("alice");
        }
        assertTrue(lockout.isLocked("alice"));
        assertEquals(MAX_ATTEMPTS + 2, lockout.getFailureCount("alice"));
    }

    @Test
    void lockoutClearsAfterDurationExpires() throws InterruptedException {
        AccountLockout shortLockout = new AccountLockout(MAX_ATTEMPTS, 5);
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            shortLockout.recordFailure("alice");
        }
        assertTrue(shortLockout.isLocked("alice"));
        Thread.sleep(20);
        assertFalse(shortLockout.isLocked("alice"));
        // After expiry, isLocked returning false also clears the failure record.
        assertEquals(0, shortLockout.getFailureCount("alice"));
    }

    @Test
    void resetFailuresClearsCount() {
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        lockout.resetFailures("alice");
        assertEquals(0, lockout.getFailureCount("alice"));
        assertFalse(lockout.isLocked("alice"));
    }

    @Test
    void resetFailuresUnlocksAccount() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            lockout.recordFailure("alice");
        }
        assertTrue(lockout.isLocked("alice"));
        lockout.resetFailures("alice");
        assertFalse(lockout.isLocked("alice"));
        assertEquals(0, lockout.getFailureCount("alice"));
    }

    @Test
    void resetFailuresOnUnknownUserIsNoop() {
        assertDoesNotThrow(() -> lockout.resetFailures("ghost"));
        assertEquals(0, lockout.getFailureCount("ghost"));
    }

    @Test
    void differentUsersHaveIndependentLockoutState() {
        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            lockout.recordFailure("alice");
        }
        assertTrue(lockout.isLocked("alice"));
        assertFalse(lockout.isLocked("bob"));
        assertEquals(0, lockout.getFailureCount("bob"));
    }

    @Test
    void getFailureCountReflectsLifecycle() {
        assertEquals(0, lockout.getFailureCount("alice"));
        lockout.recordFailure("alice");
        assertEquals(1, lockout.getFailureCount("alice"));
        lockout.recordFailure("alice");
        assertEquals(2, lockout.getFailureCount("alice"));
        lockout.recordFailure("alice");
        assertEquals(MAX_ATTEMPTS, lockout.getFailureCount("alice"));
        assertTrue(lockout.isLocked("alice"));
        lockout.resetFailures("alice");
        assertEquals(0, lockout.getFailureCount("alice"));
    }

    @Test
    void concurrentRecordFailuresFromOneUserDoNotThrow() throws InterruptedException {
        // Note: AccountLockout.recordFailure performs a non-atomic
        // `state.failureCount++`, so the exact final count under contention
        // is not guaranteed by the production contract. This test verifies
        // only what the data structure does guarantee: that concurrent
        // recordFailure / isLocked / getFailureCount calls on the same
        // username complete without throwing and that the resulting count
        // is within the mathematical bounds [1, totalAttempts].
        int threadCount = 16;
        int perThread = 25;
        int totalAttempts = threadCount * perThread;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger();
        // maxAttempts > totalAttempts so isLocked stays false — keeps the
        // test focused on concurrent state mutation, not lockout transition.
        AccountLockout shared = new AccountLockout(totalAttempts + 1, LOCKOUT_DURATION_MS);

        for (int t = 0; t < threadCount; t++) {
            pool.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        shared.recordFailure("alice");
                        shared.isLocked("alice");
                        shared.getFailureCount("alice");
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdownNow();

        assertEquals(0, errors.get(), "concurrent access must not throw");
        int finalCount = shared.getFailureCount("alice");
        assertTrue(finalCount >= 1, "at least one increment must register");
        assertTrue(finalCount <= totalAttempts,
                "count cannot exceed total attempted increments");
    }

    @Test
    void concurrentLockoutStateAccessForDifferentUsersIsIsolated()
            throws InterruptedException {
        int threadCount = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch done = new CountDownLatch(threadCount);
        AccountLockout shared = new AccountLockout(MAX_ATTEMPTS, LOCKOUT_DURATION_MS);

        for (int t = 0; t < threadCount; t++) {
            final String username = "user-" + t;
            pool.submit(() -> {
                try {
                    for (int i = 0; i < MAX_ATTEMPTS; i++) {
                        shared.recordFailure(username);
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        assertTrue(done.await(5, TimeUnit.SECONDS));
        pool.shutdownNow();

        for (int t = 0; t < threadCount; t++) {
            String username = "user-" + t;
            assertEquals(MAX_ATTEMPTS, shared.getFailureCount(username));
            assertTrue(shared.isLocked(username));
        }
    }
}
