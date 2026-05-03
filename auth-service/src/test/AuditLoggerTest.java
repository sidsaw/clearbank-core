import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AuditLoggerTest {

    private AuditLogger logger;

    @BeforeEach
    void setUp() {
        logger = new AuditLogger();
    }

    @Test
    void newLoggerHasNoEntries() {
        assertTrue(logger.getEntries().isEmpty());
    }

    @Test
    void logRecordsEntryWithTypeUsernameAndMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("ip", "127.0.0.1");
        metadata.put("userAgent", "test-agent");

        Instant before = Instant.now();
        logger.log("LOGIN_SUCCESS", "alice", metadata);
        Instant after = Instant.now();

        List<AuditLogger.AuditEntry> entries = logger.getEntries();
        assertEquals(1, entries.size());
        AuditLogger.AuditEntry entry = entries.get(0);
        assertEquals("LOGIN_SUCCESS", entry.eventType);
        assertEquals("alice", entry.username);
        assertEquals("127.0.0.1", entry.metadata.get("ip"));
        assertEquals("test-agent", entry.metadata.get("userAgent"));
        assertNotNull(entry.timestamp);
        assertFalse(entry.timestamp.isBefore(before));
        assertFalse(entry.timestamp.isAfter(after));
    }

    @Test
    void logAcceptsEmptyMetadata() {
        logger.log("LOGOUT", "bob", new HashMap<>());
        List<AuditLogger.AuditEntry> entries = logger.getEntries();
        assertEquals(1, entries.size());
        assertTrue(entries.get(0).metadata.isEmpty());
    }

    @Test
    void multipleEventsAreRecordedInOrder() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        logger.log("TOKEN_REFRESH", "alice", Map.of());
        logger.log("LOGOUT", "alice", Map.of());

        List<AuditLogger.AuditEntry> entries = logger.getEntries();
        assertEquals(3, entries.size());
        assertEquals("LOGIN_SUCCESS", entries.get(0).eventType);
        assertEquals("TOKEN_REFRESH", entries.get(1).eventType);
        assertEquals("LOGOUT", entries.get(2).eventType);
    }

    @Test
    void getEntriesForUserFiltersByUsername() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        logger.log("LOGIN_SUCCESS", "bob", Map.of());
        logger.log("LOGOUT", "alice", Map.of());

        List<AuditLogger.AuditEntry> aliceEntries = logger.getEntriesForUser("alice");
        assertEquals(2, aliceEntries.size());
        assertTrue(aliceEntries.stream().allMatch(e -> e.username.equals("alice")));

        List<AuditLogger.AuditEntry> bobEntries = logger.getEntriesForUser("bob");
        assertEquals(1, bobEntries.size());
        assertEquals("bob", bobEntries.get(0).username);
    }

    @Test
    void getEntriesForUserReturnsEmptyWhenUnknown() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        assertTrue(logger.getEntriesForUser("ghost").isEmpty());
    }

    @Test
    void getEntriesByTypeFiltersByEventType() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        logger.log("LOGIN_FAILED", "alice", Map.of());
        logger.log("LOGIN_SUCCESS", "bob", Map.of());
        logger.log("LOGOUT", "alice", Map.of());

        List<AuditLogger.AuditEntry> successes = logger.getEntriesByType("LOGIN_SUCCESS");
        assertEquals(2, successes.size());
        assertTrue(successes.stream()
                .allMatch(e -> e.eventType.equals("LOGIN_SUCCESS")));

        assertEquals(1, logger.getEntriesByType("LOGIN_FAILED").size());
        assertEquals(1, logger.getEntriesByType("LOGOUT").size());
    }

    @Test
    void getEntriesByTypeReturnsEmptyForUnknownType() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        assertTrue(logger.getEntriesByType("NONEXISTENT_EVENT").isEmpty());
    }

    @Test
    void getEntriesIsUnmodifiable() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        List<AuditLogger.AuditEntry> entries = logger.getEntries();
        AuditLogger.AuditEntry sample = entries.get(0);
        assertThrows(UnsupportedOperationException.class, () -> entries.add(sample));
        assertThrows(UnsupportedOperationException.class, () -> entries.remove(0));
        assertThrows(UnsupportedOperationException.class, entries::clear);
    }

    @Test
    void getEntriesViewReflectsSubsequentLogs() {
        // unmodifiableList returns a live view, so existing references must
        // observe newly logged entries.
        List<AuditLogger.AuditEntry> view = logger.getEntries();
        assertTrue(view.isEmpty());
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        assertEquals(1, view.size());
    }

    @Test
    void clearEmptiesAllEntries() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        logger.log("LOGIN_FAILED", "bob", Map.of());
        assertEquals(2, logger.getEntries().size());

        logger.clear();
        assertTrue(logger.getEntries().isEmpty());
        assertTrue(logger.getEntriesForUser("alice").isEmpty());
        assertTrue(logger.getEntriesByType("LOGIN_SUCCESS").isEmpty());
    }

    @Test
    void clearOnEmptyLogIsNoop() {
        assertDoesNotThrow(() -> logger.clear());
        assertTrue(logger.getEntries().isEmpty());
    }

    @Test
    void logAfterClearStartsFresh() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        logger.clear();
        logger.log("LOGOUT", "bob", Map.of());

        List<AuditLogger.AuditEntry> entries = logger.getEntries();
        assertEquals(1, entries.size());
        assertEquals("LOGOUT", entries.get(0).eventType);
        assertEquals("bob", entries.get(0).username);
    }
}
