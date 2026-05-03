import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AuditLoggerTest {

    private AuditLogger auditLogger;

    @BeforeEach
    void setUp() {
        auditLogger = new AuditLogger();
    }

    @Test
    void freshLoggerHasNoEntries() {
        assertTrue(auditLogger.getEntries().isEmpty());
    }

    @Test
    void logRecordsEntryWithCorrectFields() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("ip", "127.0.0.1");
        auditLogger.log("LOGIN_SUCCESS", "alice", metadata);

        List<AuditLogger.AuditEntry> entries = auditLogger.getEntries();
        assertEquals(1, entries.size());

        AuditLogger.AuditEntry entry = entries.get(0);
        assertEquals("LOGIN_SUCCESS", entry.eventType);
        assertEquals("alice", entry.username);
        assertEquals("127.0.0.1", entry.metadata.get("ip"));
        assertNotNull(entry.timestamp);
    }

    @Test
    void logAcceptsEmptyMetadata() {
        auditLogger.log("LOGOUT", "alice", new HashMap<>());
        AuditLogger.AuditEntry entry = auditLogger.getEntries().get(0);
        assertTrue(entry.metadata.isEmpty());
    }

    @Test
    void logAcceptsNullMetadata() {
        auditLogger.log("TOKEN_REFRESH", "alice", null);
        AuditLogger.AuditEntry entry = auditLogger.getEntries().get(0);
        assertNull(entry.metadata);
    }

    @Test
    void logPreservesInsertionOrder() {
        auditLogger.log("LOGIN_SUCCESS", "alice", null);
        auditLogger.log("LOGOUT", "alice", null);
        auditLogger.log("LOGIN_FAILED", "bob", null);

        List<AuditLogger.AuditEntry> entries = auditLogger.getEntries();
        assertEquals("LOGIN_SUCCESS", entries.get(0).eventType);
        assertEquals("LOGOUT", entries.get(1).eventType);
        assertEquals("LOGIN_FAILED", entries.get(2).eventType);
    }

    @Test
    void getEntriesForUserReturnsOnlyMatchingUser() {
        auditLogger.log("LOGIN_SUCCESS", "alice", null);
        auditLogger.log("LOGIN_FAILED", "bob", null);
        auditLogger.log("LOGOUT", "alice", null);

        List<AuditLogger.AuditEntry> aliceEntries =
                auditLogger.getEntriesForUser("alice");
        assertEquals(2, aliceEntries.size());
        for (AuditLogger.AuditEntry entry : aliceEntries) {
            assertEquals("alice", entry.username);
        }
    }

    @Test
    void getEntriesForUserReturnsEmptyForUnknownUser() {
        auditLogger.log("LOGIN_SUCCESS", "alice", null);
        assertTrue(auditLogger.getEntriesForUser("nobody").isEmpty());
    }

    @Test
    void getEntriesByTypeReturnsOnlyMatchingType() {
        auditLogger.log("LOGIN_SUCCESS", "alice", null);
        auditLogger.log("LOGIN_FAILED", "bob", null);
        auditLogger.log("LOGIN_SUCCESS", "carol", null);

        List<AuditLogger.AuditEntry> successes =
                auditLogger.getEntriesByType("LOGIN_SUCCESS");
        assertEquals(2, successes.size());
        for (AuditLogger.AuditEntry entry : successes) {
            assertEquals("LOGIN_SUCCESS", entry.eventType);
        }
    }

    @Test
    void getEntriesByTypeReturnsEmptyForUnknownType() {
        auditLogger.log("LOGIN_SUCCESS", "alice", null);
        assertTrue(auditLogger.getEntriesByType("PASSWORD_RESET").isEmpty());
    }

    @Test
    void getEntriesReturnsUnmodifiableList() {
        auditLogger.log("LOGIN_SUCCESS", "alice", null);
        List<AuditLogger.AuditEntry> entries = auditLogger.getEntries();
        AuditLogger.AuditEntry sample = entries.get(0);
        assertThrows(UnsupportedOperationException.class,
                () -> entries.add(sample));
        assertThrows(UnsupportedOperationException.class,
                () -> entries.remove(0));
        assertThrows(UnsupportedOperationException.class, entries::clear);
    }

    @Test
    void clearEmptiesTheLog() {
        auditLogger.log("LOGIN_SUCCESS", "alice", null);
        auditLogger.log("LOGOUT", "alice", null);
        assertEquals(2, auditLogger.getEntries().size());

        auditLogger.clear();
        assertTrue(auditLogger.getEntries().isEmpty());
        assertTrue(auditLogger.getEntriesForUser("alice").isEmpty());
        assertTrue(auditLogger.getEntriesByType("LOGIN_SUCCESS").isEmpty());
    }

    @Test
    void clearOnEmptyLogIsNoop() {
        assertDoesNotThrow(() -> auditLogger.clear());
        assertTrue(auditLogger.getEntries().isEmpty());
    }

    @Test
    void logEntriesAreVisibleAfterClearAndRelog() {
        auditLogger.log("LOGIN_SUCCESS", "alice", null);
        auditLogger.clear();
        auditLogger.log("LOGIN_FAILED", "bob", null);

        List<AuditLogger.AuditEntry> entries = auditLogger.getEntries();
        assertEquals(1, entries.size());
        assertEquals("LOGIN_FAILED", entries.get(0).eventType);
        assertEquals("bob", entries.get(0).username);
    }
}
