import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {

    private final Map<String, UserRecord> users = new ConcurrentHashMap<>();

    public UserRepository() {
        users.put("alice", new UserRecord("alice", "password123", "ADMIN"));
        users.put("bob", new UserRecord("bob", "hunter2", "USER"));
        users.put("carol", new UserRecord("carol", "letmein", "USER"));
    }

    public String getPassword(String username) {
        UserRecord record = users.get(username);
        return record != null ? record.password : null;
    }

    public String getRole(String username) {
        UserRecord record = users.get(username);
        return record != null ? record.role : null;
    }

    public boolean exists(String username) {
        return users.containsKey(username);
    }

    public void addUser(String username, String password, String role) {
        if (users.containsKey(username)) {
            throw new IllegalArgumentException("User already exists: " + username);
        }
        users.put(username, new UserRecord(username, password, role));
    }

    public void removeUser(String username) {
        users.remove(username);
    }

    private static class UserRecord {
        final String username;
        final String password;
        final String role;

        UserRecord(String username, String password, String role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }
    }
}
