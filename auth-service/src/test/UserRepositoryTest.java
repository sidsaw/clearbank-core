import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UserRepositoryTest {

    private UserRepository repo;

    @BeforeEach
    void setUp() {
        repo = new UserRepository();
    }

    @Test
    void preloadedUserExists() {
        assertTrue(repo.exists("alice"));
        assertTrue(repo.exists("bob"));
        assertTrue(repo.exists("carol"));
    }

    @Test
    void unknownUserDoesNotExist() {
        assertFalse(repo.exists("unknown"));
    }

    @Test
    void getPasswordReturnsCorrectValue() {
        assertEquals("password123", repo.getPassword("alice"));
    }

    @Test
    void getPasswordReturnsNullForUnknownUser() {
        assertNull(repo.getPassword("nobody"));
    }

    @Test
    void getRoleReturnsCorrectRole() {
        assertEquals("ADMIN", repo.getRole("alice"));
        assertEquals("USER", repo.getRole("bob"));
    }

    @Test
    void addUserMakesUserExist() {
        repo.addUser("dave", "pass", "USER");
        assertTrue(repo.exists("dave"));
    }

    @Test
    void addDuplicateUserThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> repo.addUser("alice", "x", "USER"));
    }

    @Test
    void removeUserMakesUserDisappear() {
        repo.removeUser("alice");
        assertFalse(repo.exists("alice"));
    }
}
