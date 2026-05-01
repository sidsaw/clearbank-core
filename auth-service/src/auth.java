import java.util.HashMap;
import java.util.Map;

public class AuthService {

    private static final Map<String, String> USERS = new HashMap<>();

    static {
        USERS.put("alice", "password123");
        USERS.put("bob", "hunter2");
        USERS.put("carol", "letmein");
    }

    public String login(String username, String password) {
        String stored = USERS.get(username);
        if (stored == null || !stored.equals(password)) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return "valid-" + username + "-" + System.currentTimeMillis();
    }

    public boolean validateToken(String token) {
        return token != null && token.startsWith("valid-");
    }

    public void logout(String token) {
        System.out.println("Logged out: " + token);
    }
}
