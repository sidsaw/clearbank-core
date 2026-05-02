import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AccountLockout {

    private final int maxAttempts;
    private final long lockoutDurationMs;
    private final Map<String, LockoutState> states = new ConcurrentHashMap<>();

    public AccountLockout(int maxAttempts, long lockoutDurationMs) {
        this.maxAttempts = maxAttempts;
        this.lockoutDurationMs = lockoutDurationMs;
    }

    public boolean isLocked(String username) {
        LockoutState state = states.get(username);
        if (state == null) {
            return false;
        }
        if (state.lockedUntil > 0 && System.currentTimeMillis() < state.lockedUntil) {
            return true;
        }
        if (state.lockedUntil > 0 && System.currentTimeMillis() >= state.lockedUntil) {
            states.remove(username);
            return false;
        }
        return false;
    }

    public void recordFailure(String username) {
        LockoutState state = states.computeIfAbsent(username, k -> new LockoutState());
        state.failureCount++;
        if (state.failureCount >= maxAttempts) {
            state.lockedUntil = System.currentTimeMillis() + lockoutDurationMs;
        }
    }

    public void resetFailures(String username) {
        states.remove(username);
    }

    public int getFailureCount(String username) {
        LockoutState state = states.get(username);
        return state != null ? state.failureCount : 0;
    }

    private static class LockoutState {
        int failureCount;
        long lockedUntil;
    }
}
