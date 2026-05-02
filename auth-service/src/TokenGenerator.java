import java.security.SecureRandom;
import java.util.Base64;

public class TokenGenerator {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private final SecureRandom random;

    public TokenGenerator() {
        this.random = new SecureRandom();
    }

    public TokenGenerator(SecureRandom random) {
        this.random = random;
    }

    public String generate(String username) {
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Username must not be empty");
        }
        byte[] bytes = new byte[TOKEN_BYTE_LENGTH];
        random.nextBytes(bytes);
        String encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return username + ":" + encoded;
    }

    public String extractUsername(String token) {
        if (token == null || !token.contains(":")) {
            return null;
        }
        return token.substring(0, token.indexOf(':'));
    }
}
