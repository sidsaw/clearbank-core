import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PermissionCheckerTest {

    private UserRepository userRepo;
    private PermissionChecker checker;

    @BeforeEach
    void setUp() {
        userRepo = new UserRepository();
        checker = new PermissionChecker(userRepo);
    }

    @Test
    void adminHasManageUsersPermission() {
        assertTrue(checker.hasPermission("alice", "MANAGE_USERS"));
    }

    @Test
    void regularUserLacksManageUsersPermission() {
        assertFalse(checker.hasPermission("bob", "MANAGE_USERS"));
    }

    @Test
    void regularUserHasReadAccountPermission() {
        assertTrue(checker.hasPermission("bob", "READ_ACCOUNT"));
    }

    @Test
    void unknownUserHasNoPermissions() {
        assertFalse(checker.hasPermission("ghost", "READ_ACCOUNT"));
    }

    @Test
    void requirePermissionThrowsWhenLacking() {
        assertThrows(SecurityException.class,
                () -> checker.requirePermission("bob", "MANAGE_USERS"));
    }

    @Test
    void requirePermissionPassesForAdmin() {
        assertDoesNotThrow(
                () -> checker.requirePermission("alice", "MANAGE_USERS"));
    }

    @Test
    void getPermissionsReturnsRolePerms() {
        assertFalse(checker.getPermissions("ADMIN").isEmpty());
    }

    @Test
    void getPermissionsReturnsEmptyForUnknownRole() {
        assertTrue(checker.getPermissions("NONEXISTENT").isEmpty());
    }
}
