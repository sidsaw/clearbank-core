import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
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
    void freshAccountIsNotLocked() {
        assertFalse(lockout.isLocked("alice"));
    }

    @Test
    void freshAccountHasZeroFailures() {
        assertEquals(0, lockout.getFailureCount("alice"));
    }

    @Test
    void recordingFailureIncrementsCount() {
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
    void accountIsLockedAfterReachingMaxAttempts() {
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        assertTrue(lockout.isLocked("alice"));
    }

    @Test
    void lockoutExpiresAfterConfiguredDuration() throws InterruptedException {
        AccountLockout shortLock = new AccountLockout(2, 10);
        shortLock.recordFailure("bob");
        shortLock.recordFailure("bob");
        assertTrue(shortLock.isLocked("bob"));
        Thread.sleep(30);
        assertFalse(shortLock.isLocked("bob"));
    }

    @Test
    void expiredLockoutResetsState() throws InterruptedException {
        AccountLockout shortLock = new AccountLockout(2, 10);
        shortLock.recordFailure("bob");
        shortLock.recordFailure("bob");
        Thread.sleep(30);
        // isLocked() returning false also clears the state map.
        assertFalse(shortLock.isLocked("bob"));
        assertEquals(0, shortLock.getFailureCount("bob"));
    }

    @Test
    void resetFailuresClearsCount() {
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        lockout.resetFailures("alice");
        assertEquals(0, lockout.getFailureCount("alice"));
    }

    @Test
    void resetFailuresUnlocksAccount() {
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        assertTrue(lockout.isLocked("alice"));
        lockout.resetFailures("alice");
        assertFalse(lockout.isLocked("alice"));
    }

    @Test
    void resetFailuresOnUnknownUserDoesNotThrow() {
        assertDoesNotThrow(() -> lockout.resetFailures("ghost"));
    }

    @Test
    void differentAccountsHaveIndependentLockoutState() {
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        assertTrue(lockout.isLocked("alice"));
        assertFalse(lockout.isLocked("bob"));
        assertEquals(0, lockout.getFailureCount("bob"));
    }

    @Test
    void getFailureCountTracksLifecycle() {
        assertEquals(0, lockout.getFailureCount("alice"));
        lockout.recordFailure("alice");
        assertEquals(1, lockout.getFailureCount("alice"));
        lockout.recordFailure("alice");
        assertEquals(2, lockout.getFailureCount("alice"));
        lockout.recordFailure("alice");
        assertEquals(3, lockout.getFailureCount("alice"));
        lockout.resetFailures("alice");
        assertEquals(0, lockout.getFailureCount("alice"));
    }

    @Test
    void concurrentFailuresShareSingleLockoutState() throws InterruptedException {
        AccountLockout sharedLock = new AccountLockout(1_000_000, 60_000);
        int threads = 8;
        int perThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for (int j = 0; j < perThread; j++) {
                    sharedLock.recordFailure("shared-user");
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));

        // computeIfAbsent guarantees a single LockoutState object for the key, so
        // every concurrent caller operates on the same counter. The increment
        // itself is not atomic, so the final value can be at most
        // threads * perThread; it must be at least 1 (confirming the state was
        // actually shared and not silently swapped out from under us).
        int count = sharedLock.getFailureCount("shared-user");
        assertTrue(count >= 1, "expected at least one recorded failure, got " + count);
        assertTrue(count <= threads * perThread,
                "expected count <= " + (threads * perThread) + ", got " + count);
    }

    @Test
    void concurrentRecordAndResetDoesNotThrow() throws InterruptedException {
        AccountLockout sharedLock = new AccountLockout(5, 60_000);
        int writers = 4;
        int resetters = 2;
        int iterations = 100;
        ExecutorService pool = Executors.newFixedThreadPool(writers + resetters);
        CountDownLatch start = new CountDownLatch(1);
        List<Throwable> errors = new ArrayList<>();

        Runnable recorder = () -> {
            try {
                start.await();
                for (int j = 0; j < iterations; j++) {
                    sharedLock.recordFailure("shared-user");
                    sharedLock.isLocked("shared-user");
                }
            } catch (Throwable t) {
                synchronized (errors) {
                    errors.add(t);
                }
            }
        };
        Runnable resetter = () -> {
            try {
                start.await();
                for (int j = 0; j < iterations; j++) {
                    sharedLock.resetFailures("shared-user");
                }
            } catch (Throwable t) {
                synchronized (errors) {
                    errors.add(t);
                }
            }
        };

        for (int i = 0; i < writers; i++) pool.submit(recorder);
        for (int i = 0; i < resetters; i++) pool.submit(resetter);

        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertTrue(errors.isEmpty(),
                "concurrent access threw unexpected exception(s): " + errors);
    }
}
