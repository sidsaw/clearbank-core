import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuthTest {

    @Test
    void validateTokenReturnsTrueForValidPrefix() {
        AuthService auth = new AuthService();
        assertTrue(auth.validateToken("valid-abc"));
    }
}
