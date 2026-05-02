import java.util.Map;

public class AuthService {

    private final UserRepository userRepo;
    private final TokenGenerator tokenGen;
    private final SessionManager sessions;
    private final AuditLogger auditLogger;
    private final AccountLockout lockout;
    private final RateLimiter rateLimiter;

    public AuthService(UserRepository userRepo, TokenGenerator tokenGen,
                       SessionManager sessions, AuditLogger auditLogger,
                       AccountLockout lockout, RateLimiter rateLimiter) {
        this.userRepo = userRepo;
        this.tokenGen = tokenGen;
        this.sessions = sessions;
        this.auditLogger = auditLogger;
        this.lockout = lockout;
        this.rateLimiter = rateLimiter;
    }

    public String login(String username, String password, String ipAddress) {
        rateLimiter.checkRate(ipAddress);

        if (lockout.isLocked(username)) {
            auditLogger.log("LOGIN_BLOCKED", username,
                    Map.of("reason", "account_locked"));
            throw new IllegalStateException("Account is locked: " + username);
        }

        String stored = userRepo.getPassword(username);
        if (stored == null || !stored.equals(password)) {
            lockout.recordFailure(username);
            auditLogger.log("LOGIN_FAILED", username,
                    Map.of("ip", ipAddress));
            throw new IllegalArgumentException("Invalid credentials");
        }

        lockout.resetFailures(username);
        String token = tokenGen.generate(username);
        sessions.create(username, token);
        auditLogger.log("LOGIN_SUCCESS", username,
                Map.of("ip", ipAddress));
        return token;
    }

    public boolean validateToken(String token) {
        if (token == null) {
            return false;
        }
        return sessions.isValid(token);
    }

    public void logout(String token) {
        if (token == null) {
            return;
        }
        String username = sessions.getUserForToken(token);
        sessions.invalidate(token);
        if (username != null) {
            auditLogger.log("LOGOUT", username, Map.of());
        }
    }

    public String refreshToken(String existingToken) {
        if (!validateToken(existingToken)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }
        String username = sessions.getUserForToken(existingToken);
        sessions.invalidate(existingToken);
        String newToken = tokenGen.generate(username);
        sessions.create(username, newToken);
        auditLogger.log("TOKEN_REFRESH", username, Map.of());
        return newToken;
    }
}
