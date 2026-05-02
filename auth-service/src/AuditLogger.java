import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class AuditLogger {

    private final List<AuditEntry> entries = new ArrayList<>();

    public void log(String eventType, String username, Map<String, String> metadata) {
        entries.add(new AuditEntry(eventType, username, metadata, Instant.now()));
    }

    public List<AuditEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    public List<AuditEntry> getEntriesForUser(String username) {
        List<AuditEntry> result = new ArrayList<>();
        for (AuditEntry entry : entries) {
            if (entry.username.equals(username)) {
                result.add(entry);
            }
        }
        return result;
    }

    public List<AuditEntry> getEntriesByType(String eventType) {
        List<AuditEntry> result = new ArrayList<>();
        for (AuditEntry entry : entries) {
            if (entry.eventType.equals(eventType)) {
                result.add(entry);
            }
        }
        return result;
    }

    public void clear() {
        entries.clear();
    }

    public static class AuditEntry {
        public final String eventType;
        public final String username;
        public final Map<String, String> metadata;
        public final Instant timestamp;

        public AuditEntry(String eventType, String username,
                          Map<String, String> metadata, Instant timestamp) {
            this.eventType = eventType;
            this.username = username;
            this.metadata = metadata;
            this.timestamp = timestamp;
        }
    }
}
