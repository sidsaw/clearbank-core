import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager(3600_000);
    }

    @Test
    void createdSessionIsValid() {
        sessionManager.create("alice", "token-1");
        assertTrue(sessionManager.isValid("token-1"));
    }

    @Test
    void unknownTokenIsInvalid() {
        assertFalse(sessionManager.isValid("nonexistent"));
    }

    @Test
    void invalidatedSessionIsNoLongerValid() {
        sessionManager.create("alice", "token-1");
        sessionManager.invalidate("token-1");
        assertFalse(sessionManager.isValid("token-1"));
    }

    @Test
    void getUserForTokenReturnsCorrectUser() {
        sessionManager.create("bob", "token-bob");
        assertEquals("bob", sessionManager.getUserForToken("token-bob"));
    }

    @Test
    void getUserForTokenReturnsNullForUnknown() {
        assertNull(sessionManager.getUserForToken("unknown"));
    }

    @Test
    void activeSessionCountReflectsCreatedSessions() {
        sessionManager.create("alice", "t1");
        sessionManager.create("bob", "t2");
        assertEquals(2, sessionManager.activeSessionCount());
    }

    @Test
    void invalidateReducesActiveCount() {
        sessionManager.create("alice", "t1");
        sessionManager.create("bob", "t2");
        sessionManager.invalidate("t1");
        assertEquals(1, sessionManager.activeSessionCount());
    }

    @Test
    void expiredSessionIsInvalid() {
        SessionManager shortLived = new SessionManager(1);
        shortLived.create("alice", "token-short");
        try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        assertFalse(shortLived.isValid("token-short"));
    }
}
