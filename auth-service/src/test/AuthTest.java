import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuthTest {

    private final AuthService auth = new AuthService();

    // --- validateToken tests ---

    @Test
    void validateTokenReturnsTrueForValidPrefix() {
        assertTrue(auth.validateToken("valid-abc"));
    }

    @Test
    void validateTokenReturnsFalseForNull() {
        assertFalse(auth.validateToken(null));
    }

    @Test
    void validateTokenReturnsFalseForInvalidPrefix() {
        assertFalse(auth.validateToken("expired-abc"));
    }

    // --- login tests ---

    @Test
    void loginReturnsTokenForValidCredentials() {
        String token = auth.login("alice", "password123");
        assertNotNull(token);
        assertTrue(token.startsWith("valid-alice-"));
    }

    @Test
    void loginThrowsForUnknownUser() {
        assertThrows(IllegalArgumentException.class,
                () -> auth.login("unknown", "password123"));
    }

    @Test
    void loginThrowsForWrongPassword() {
        assertThrows(IllegalArgumentException.class,
                () -> auth.login("alice", "wrongpassword"));
    }

    // --- logout tests ---

    @Test
    void logoutDoesNotThrow() {
        String token = auth.login("bob", "hunter2");
        assertDoesNotThrow(() -> auth.logout(token));
    }
}
