import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountResolverTest {

    private AccountResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AccountResolver();
    }

    @Test
    @DisplayName("resolve returns the AccountInfo for a known account")
    void resolvesKnownAccount() {
        AccountResolver.AccountInfo info = resolver.resolve("ACC001");

        assertNotNull(info);
        assertEquals("ACC001", info.id);
        assertEquals("Alice Johnson", info.ownerName);
        assertEquals("CHECKING", info.type);
        assertTrue(info.active);
    }

    @Test
    @DisplayName("resolve throws for an unknown account")
    void resolveThrowsForUnknownAccount() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> resolver.resolve("ACC999")
        );
        assertTrue(ex.getMessage().contains("ACC999"));
    }

    @Test
    @DisplayName("isActive returns true for active accounts and false for unknown ones")
    void isActiveDistinguishesKnownAndUnknown() {
        assertTrue(resolver.isActive("ACC001"));
        assertTrue(resolver.isActive("ACC002"));
        assertFalse(resolver.isActive("ACC999"));
    }

    @Test
    @DisplayName("getAccountType returns the configured type or null for unknown accounts")
    void getAccountTypeBehavior() {
        assertEquals("CHECKING", resolver.getAccountType("ACC001"));
        assertEquals("SAVINGS", resolver.getAccountType("ACC002"));
        assertEquals("CHECKING", resolver.getAccountType("ACC003"));
        assertNull(resolver.getAccountType("ACC999"));
    }
}
