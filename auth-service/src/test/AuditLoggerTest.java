import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void logRecordsEventTypeAndUsername() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        List<AuditLogger.AuditEntry> entries = logger.getEntries();
        assertEquals(1, entries.size());
        assertEquals("LOGIN_SUCCESS", entries.get(0).eventType);
        assertEquals("alice", entries.get(0).username);
    }

    @Test
    void logRecordsMetadata() {
        Map<String, String> metadata = Map.of("ip", "10.0.0.1", "userAgent", "test-agent");
        logger.log("LOGIN_SUCCESS", "alice", metadata);
        AuditLogger.AuditEntry entry = logger.getEntries().get(0);
        assertEquals("10.0.0.1", entry.metadata.get("ip"));
        assertEquals("test-agent", entry.metadata.get("userAgent"));
    }

    @Test
    void logRecordsTimestamp() {
        logger.log("LOGOUT", "alice", Map.of());
        AuditLogger.AuditEntry entry = logger.getEntries().get(0);
        assertNotNull(entry.timestamp);
    }

    @Test
    void multipleEventsArePreservedInOrder() {
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
        logger.log("LOGIN_FAILURE", "bob", Map.of());
        logger.log("LOGOUT", "alice", Map.of());

        List<AuditLogger.AuditEntry> aliceEntries = logger.getEntriesForUser("alice");
        assertEquals(2, aliceEntries.size());
        assertTrue(aliceEntries.stream().allMatch(e -> e.username.equals("alice")));
    }

    @Test
    void getEntriesForUserReturnsEmptyForUnknownUser() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        assertTrue(logger.getEntriesForUser("ghost").isEmpty());
    }

    @Test
    void getEntriesByTypeFiltersByEventType() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        logger.log("LOGIN_FAILURE", "bob", Map.of());
        logger.log("LOGIN_SUCCESS", "carol", Map.of());

        List<AuditLogger.AuditEntry> successes = logger.getEntriesByType("LOGIN_SUCCESS");
        assertEquals(2, successes.size());
        assertTrue(successes.stream().allMatch(e -> e.eventType.equals("LOGIN_SUCCESS")));
    }

    @Test
    void getEntriesByTypeReturnsEmptyForUnknownType() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        assertTrue(logger.getEntriesByType("UNKNOWN_EVENT").isEmpty());
    }

    @Test
    void getEntriesReturnsUnmodifiableList() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        List<AuditLogger.AuditEntry> entries = logger.getEntries();
        assertThrows(UnsupportedOperationException.class,
                () -> entries.add(new AuditLogger.AuditEntry(
                        "INJECTED", "mallory", Map.of(), java.time.Instant.now())));
    }

    @Test
    void getEntriesUnmodifiableViewBlocksRemove() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        List<AuditLogger.AuditEntry> entries = logger.getEntries();
        assertThrows(UnsupportedOperationException.class, () -> entries.remove(0));
    }

    @Test
    void clearEmptiesTheLog() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        logger.log("LOGOUT", "alice", Map.of());
        logger.clear();
        assertTrue(logger.getEntries().isEmpty());
    }

    @Test
    void clearOnEmptyLoggerDoesNotThrow() {
        assertDoesNotThrow(() -> logger.clear());
        assertTrue(logger.getEntries().isEmpty());
    }

    @Test
    void logAcceptsMutableMetadataMap() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("ip", "10.0.0.1");
        logger.log("LOGIN_SUCCESS", "alice", metadata);
        assertEquals(1, logger.getEntries().size());
    }

    @Test
    void filteringDoesNotMutateOriginalEntries() {
        logger.log("LOGIN_SUCCESS", "alice", Map.of());
        logger.log("LOGIN_FAILURE", "bob", Map.of());
        logger.getEntriesForUser("alice");
        logger.getEntriesByType("LOGIN_SUCCESS");
        assertEquals(2, logger.getEntries().size());
    }
}
