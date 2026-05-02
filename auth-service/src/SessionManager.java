import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {

    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    private final long sessionTtlMs;

    public SessionManager(long sessionTtlMs) {
        this.sessionTtlMs = sessionTtlMs;
    }

    public void create(String username, String token) {
        activeSessions.put(token, new SessionInfo(username, System.currentTimeMillis()));
    }

    public boolean isValid(String token) {
        SessionInfo info = activeSessions.get(token);
        if (info == null) {
            return false;
        }
        if (System.currentTimeMillis() - info.createdAt > sessionTtlMs) {
            activeSessions.remove(token);
            return false;
        }
        return true;
    }

    public String getUserForToken(String token) {
        SessionInfo info = activeSessions.get(token);
        return info != null ? info.username : null;
    }

    public void invalidate(String token) {
        activeSessions.remove(token);
    }

    public int activeSessionCount() {
        return activeSessions.size();
    }

    private static class SessionInfo {
        final String username;
        final long createdAt;

        SessionInfo(String username, long createdAt) {
            this.username = username;
            this.createdAt = createdAt;
        }
    }
}
