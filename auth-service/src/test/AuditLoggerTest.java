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
    void logAddsEntryWithCorrectFields() {
        Map<String, String> meta = Map.of("ip", "127.0.0.1");

        Instant before = Instant.now();
        logger.log("LOGIN_SUCCESS", "alice", meta);
        Instant after = Instant.now();

        List<AuditLogger.AuditEntry> entries = logger.getEntries();
        assertEquals(1, entries.size());

        AuditLogger.AuditEntry entry = entries.get(0);
        assertEquals("LOGIN_SUCCESS", entry.eventType);
        assertEquals("alice", entry.username);
        assertEquals(meta, entry.metadata);
        assertNotNull(entry.timestamp);
        assertFalse(entry.timestamp.isBefore(before));
        assertFalse(entry.timestamp.isAfter(after));
    }

    @Test
    void logSupportsNullMetadata() {
        logger.log("LOGOUT", "alice", null);

        AuditLogger.AuditEntry entry = logger.getEntries().get(0);
        assertNull(entry.metadata);
    }

    @Test
    void multipleLogsAreAppendedInOrder() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        logger.log("LOGOUT", "alice", Map.of());
        logger.log("LOGIN_FAILED", "bob", Map.of());

        List<AuditLogger.AuditEntry> entries = logger.getEntries();
        assertEquals(3, entries.size());
        assertEquals("LOGIN_SUCCESS", entries.get(0).eventType);
        assertEquals("LOGOUT", entries.get(1).eventType);
        assertEquals("LOGIN_FAILED", entries.get(2).eventType);
    }

    @Test
    void getEntriesForUserReturnsOnlyMatchingUser() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        logger.log("LOGIN_SUCCESS", "bob", Map.of());
        logger.log("LOGOUT", "alice", Map.of());

        List<AuditLogger.AuditEntry> aliceEntries = logger.getEntriesForUser("alice");

        assertEquals(2, aliceEntries.size());
        for (AuditLogger.AuditEntry entry : aliceEntries) {
            assertEquals("alice", entry.username);
        }
    }

    @Test
    void getEntriesForUserReturnsEmptyListForUnknownUser() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());

        assertTrue(logger.getEntriesForUser("ghost").isEmpty());
    }

    @Test
    void getEntriesByTypeReturnsOnlyMatchingType() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        logger.log("LOGIN_FAILED", "alice", Map.of());
        logger.log("LOGIN_SUCCESS", "bob", Map.of());
        logger.log("LOGOUT", "alice", Map.of());

        List<AuditLogger.AuditEntry> successes = logger.getEntriesByType("LOGIN_SUCCESS");

        assertEquals(2, successes.size());
        for (AuditLogger.AuditEntry entry : successes) {
            assertEquals("LOGIN_SUCCESS", entry.eventType);
        }
    }

    @Test
    void getEntriesByTypeReturnsEmptyListForUnknownType() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());

        assertTrue(logger.getEntriesByType("UNKNOWN_EVENT").isEmpty());
    }

    @Test
    void getEntriesReturnsImmutableList() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());

        List<AuditLogger.AuditEntry> entries = logger.getEntries();

        AuditLogger.AuditEntry attempted = new AuditLogger.AuditEntry(
                "TAMPER", "mallory", Map.of(), Instant.now());
        assertThrows(UnsupportedOperationException.class,
                () -> entries.add(attempted));
        assertThrows(UnsupportedOperationException.class,
                () -> entries.remove(0));
    }

    @Test
    void clearEmptiesAllEntries() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        logger.log("LOGOUT", "bob", Map.of());
        assertEquals(2, logger.getEntries().size());

        logger.clear();

        assertTrue(logger.getEntries().isEmpty());
    }

    @Test
    void clearOnEmptyLoggerIsNoOp() {
        assertDoesNotThrow(() -> logger.clear());
        assertTrue(logger.getEntries().isEmpty());
    }

    @Test
    void metadataPreservesAllProvidedFields() {
        Map<String, String> meta = new HashMap<>();
        meta.put("ip", "127.0.0.1");
        meta.put("userAgent", "Mozilla/5.0");
        meta.put("sessionId", "sess-123");

        logger.log("LOGIN_SUCCESS", "alice", meta);

        AuditLogger.AuditEntry entry = logger.getEntries().get(0);
        assertEquals("127.0.0.1", entry.metadata.get("ip"));
        assertEquals("Mozilla/5.0", entry.metadata.get("userAgent"));
        assertEquals("sess-123", entry.metadata.get("sessionId"));
    }

    @Test
    void filtersDoNotMutateUnderlyingEntries() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        logger.log("LOGIN_FAILED", "bob", Map.of());

        logger.getEntriesForUser("alice");
        logger.getEntriesByType("LOGIN_SUCCESS");

        assertEquals(2, logger.getEntries().size());
    }
}
