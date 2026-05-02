import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthServiceTest {

    private UserRepository userRepo;
    private TokenGenerator tokenGen;
    private SessionManager sessions;
    private AuditLogger auditLogger;
    private AccountLockout lockout;
    private RateLimiter rateLimiter;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepo = new UserRepository();
        tokenGen = new TokenGenerator();
        sessions = new SessionManager(3600_000);
        auditLogger = new AuditLogger();
        lockout = new AccountLockout(5, 900_000);
        rateLimiter = new RateLimiter(20);
        authService = new AuthService(userRepo, tokenGen, sessions,
                auditLogger, lockout, rateLimiter);
    }

    @Test
    void loginSucceedsWithValidCredentials() {
        String token = authService.login("alice", "password123", "127.0.0.1");
        assertNotNull(token);
        assertTrue(token.startsWith("alice:"));
    }

    @Test
    void loginFailsWithWrongPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("alice", "wrong", "127.0.0.1"));
    }

    @Test
    void loginFailsWithUnknownUser() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("nobody", "pass", "127.0.0.1"));
    }

    @Test
    void validateTokenReturnsTrueForActiveSession() {
        String token = authService.login("bob", "hunter2", "127.0.0.1");
        assertTrue(authService.validateToken(token));
    }

    @Test
    void validateTokenReturnsFalseForNull() {
        assertFalse(authService.validateToken(null));
    }

    @Test
    void validateTokenReturnsFalseForBogusToken() {
        assertFalse(authService.validateToken("not-a-real-token"));
    }

    @Test
    void logoutInvalidatesToken() {
        String token = authService.login("carol", "letmein", "127.0.0.1");
        assertTrue(authService.validateToken(token));
        authService.logout(token);
        assertFalse(authService.validateToken(token));
    }

    @Test
    void logoutWithNullTokenDoesNotThrow() {
        assertDoesNotThrow(() -> authService.logout(null));
    }

    @Test
    void refreshTokenReturnsNewToken() {
        String original = authService.login("alice", "password123", "127.0.0.1");
        String refreshed = authService.refreshToken(original);
        assertNotNull(refreshed);
        assertNotEquals(original, refreshed);
        assertFalse(authService.validateToken(original));
        assertTrue(authService.validateToken(refreshed));
    }

    @Test
    void refreshTokenFailsForInvalidToken() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.refreshToken("bogus"));
    }

    @Test
    void loginRecordsAuditEntry() {
        authService.login("alice", "password123", "127.0.0.1");
        assertEquals(1, auditLogger.getEntriesByType("LOGIN_SUCCESS").size());
    }

    @Test
    void failedLoginRecordsAuditEntry() {
        try {
            authService.login("alice", "wrong", "127.0.0.1");
        } catch (IllegalArgumentException ignored) {}
        assertEquals(1, auditLogger.getEntriesByType("LOGIN_FAILED").size());
    }

    @Test
    void loginBlockedWhenAccountLocked() {
        for (int i = 0; i < 5; i++) {
            try {
                authService.login("alice", "wrong", "127.0.0.1");
            } catch (IllegalArgumentException ignored) {}
        }
        assertThrows(IllegalStateException.class,
                () -> authService.login("alice", "password123", "127.0.0.1"));
    }
}
