import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TokenGeneratorTest {

    private TokenGenerator tokenGen;

    @BeforeEach
    void setUp() {
        tokenGen = new TokenGenerator();
    }

    @Test
    void generatedTokenContainsUsername() {
        String token = tokenGen.generate("alice");
        assertTrue(token.startsWith("alice:"));
    }

    @Test
    void twoTokensAreUnique() {
        String t1 = tokenGen.generate("alice");
        String t2 = tokenGen.generate("alice");
        assertNotEquals(t1, t2);
    }

    @Test
    void generateThrowsForNullUsername() {
        assertThrows(IllegalArgumentException.class, () -> tokenGen.generate(null));
    }

    @Test
    void generateThrowsForEmptyUsername() {
        assertThrows(IllegalArgumentException.class, () -> tokenGen.generate(""));
    }

    @Test
    void extractUsernameReturnsCorrectUser() {
        String token = tokenGen.generate("bob");
        assertEquals("bob", tokenGen.extractUsername(token));
    }

    @Test
    void extractUsernameReturnsNullForInvalidToken() {
        assertNull(tokenGen.extractUsername("no-colon-here"));
    }

    @Test
    void extractUsernameReturnsNullForNull() {
        assertNull(tokenGen.extractUsername(null));
    }
}
