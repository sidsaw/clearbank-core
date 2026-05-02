import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PermissionChecker {

    private static final Map<String, Set<String>> ROLE_PERMISSIONS = new HashMap<>();

    static {
        ROLE_PERMISSIONS.put("ADMIN", Set.of(
                "READ_ACCOUNT", "WRITE_ACCOUNT", "DELETE_ACCOUNT",
                "VIEW_AUDIT_LOG", "MANAGE_USERS", "VIEW_REPORTS"
        ));
        ROLE_PERMISSIONS.put("USER", Set.of(
                "READ_ACCOUNT", "WRITE_ACCOUNT"
        ));
        ROLE_PERMISSIONS.put("AUDITOR", Set.of(
                "READ_ACCOUNT", "VIEW_AUDIT_LOG", "VIEW_REPORTS"
        ));
    }

    private final UserRepository userRepo;

    public PermissionChecker(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    public boolean hasPermission(String username, String permission) {
        String role = userRepo.getRole(username);
        if (role == null) {
            return false;
        }
        Set<String> perms = ROLE_PERMISSIONS.get(role);
        return perms != null && perms.contains(permission);
    }

    public void requirePermission(String username, String permission) {
        if (!hasPermission(username, permission)) {
            throw new SecurityException(
                    "User '" + username + "' lacks permission: " + permission);
        }
    }

    public Set<String> getPermissions(String role) {
        return ROLE_PERMISSIONS.getOrDefault(role, Set.of());
    }
}
