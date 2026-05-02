public class AuthConfig {

    private long sessionTtlMs = 3600_000;
    private int maxLoginAttempts = 5;
    private long lockoutDurationMs = 900_000;
    private int rateLimitPerMinute = 20;
    private int minPasswordLength = 8;

    public long getSessionTtlMs() { return sessionTtlMs; }
    public void setSessionTtlMs(long sessionTtlMs) { this.sessionTtlMs = sessionTtlMs; }

    public int getMaxLoginAttempts() { return maxLoginAttempts; }
    public void setMaxLoginAttempts(int maxLoginAttempts) { this.maxLoginAttempts = maxLoginAttempts; }

    public long getLockoutDurationMs() { return lockoutDurationMs; }
    public void setLockoutDurationMs(long lockoutDurationMs) { this.lockoutDurationMs = lockoutDurationMs; }

    public int getRateLimitPerMinute() { return rateLimitPerMinute; }
    public void setRateLimitPerMinute(int rateLimitPerMinute) { this.rateLimitPerMinute = rateLimitPerMinute; }

    public int getMinPasswordLength() { return minPasswordLength; }
    public void setMinPasswordLength(int minPasswordLength) { this.minPasswordLength = minPasswordLength; }
}
