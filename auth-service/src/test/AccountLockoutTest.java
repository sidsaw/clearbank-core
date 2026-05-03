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
    void newUserIsNotLocked() {
        assertFalse(lockout.isLocked("alice"));
    }

    @Test
    void newUserHasZeroFailures() {
        assertEquals(0, lockout.getFailureCount("alice"));
    }

    @Test
    void recordFailureIncrementsCount() {
        lockout.recordFailure("alice");
        assertEquals(1, lockout.getFailureCount("alice"));
    }

    @Test
    void failuresBelowThresholdDoNotLock() {
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        assertFalse(lockout.isLocked("alice"));
        assertEquals(2, lockout.getFailureCount("alice"));
    }

    @Test
    void accountLocksAfterMaxFailures() {
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        assertTrue(lockout.isLocked("alice"));
    }

    @Test
    void lockoutExpiresAfterDuration() throws InterruptedException {
        AccountLockout shortLockout = new AccountLockout(2, 10);
        shortLockout.recordFailure("alice");
        shortLockout.recordFailure("alice");
        assertTrue(shortLockout.isLocked("alice"));
        Thread.sleep(30);
        assertFalse(shortLockout.isLocked("alice"));
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
    void differentUsersHaveSeparateState() {
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        lockout.recordFailure("alice");
        assertTrue(lockout.isLocked("alice"));
        assertFalse(lockout.isLocked("bob"));
        assertEquals(0, lockout.getFailureCount("bob"));
    }

    @Test
    void failureCountTracksLifecycle() {
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
    void afterLockoutExpiryFailureCountIsCleared() throws InterruptedException {
        AccountLockout shortLockout = new AccountLockout(2, 10);
        shortLockout.recordFailure("alice");
        shortLockout.recordFailure("alice");
        assertTrue(shortLockout.isLocked("alice"));
        Thread.sleep(30);
        // isLocked clears expired state
        assertFalse(shortLockout.isLocked("alice"));
        assertEquals(0, shortLockout.getFailureCount("alice"));
    }

    @Test
    void concurrentRecordFailureLocksAccount() throws InterruptedException {
        // The underlying map is concurrent; this test verifies that under
        // concurrent failure recording the account ultimately ends up locked
        // (lockedUntil is set once the threshold is crossed) and no exceptions
        // are thrown.
        AccountLockout concurrentLockout = new AccountLockout(5, 60_000);
        int threadCount = 10;
        int failuresPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < failuresPerThread; i++) {
                        concurrentLockout.recordFailure("alice");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        assertTrue(concurrentLockout.isLocked("alice"));
        assertTrue(concurrentLockout.getFailureCount("alice") >= 5);
    }

    @Test
    void concurrentDifferentUsersTrackedIndependently() throws InterruptedException {
        AccountLockout concurrentLockout = new AccountLockout(1_000_000, 60_000);
        int userCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(userCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(userCount);
        List<String> usernames = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            usernames.add("user-" + i);
        }

        for (String username : usernames) {
            executor.submit(() -> {
                try {
                    start.await();
                    for (int i = 0; i < 5; i++) {
                        concurrentLockout.recordFailure(username);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertTrue(done.await(5, TimeUnit.SECONDS));
        executor.shutdown();

        for (String username : usernames) {
            assertEquals(5, concurrentLockout.getFailureCount(username));
        }
    }
}
