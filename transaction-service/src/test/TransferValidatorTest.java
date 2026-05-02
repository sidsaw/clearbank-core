import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferValidatorTest {

    private TransferValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TransferValidator();
    }

    @Test
    @DisplayName("accepts a transfer within the configured limits")
    void acceptsValidTransfer() {
        assertDoesNotThrow(() -> validator.validate("ACC001", "ACC002", 100.00));
    }

    @Test
    @DisplayName("accepts a transfer exactly at the per-transfer maximum")
    void acceptsTransferAtMaximum() {
        assertDoesNotThrow(() -> validator.validate("ACC001", "ACC002", 25_000.00));
    }

    @Test
    @DisplayName("rejects a null source account")
    void rejectsNullSource() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate(null, "ACC002", 100.00)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("source"));
    }

    @Test
    @DisplayName("rejects an empty source account")
    void rejectsEmptySource() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate("", "ACC002", 100.00)
        );
    }

    @Test
    @DisplayName("rejects a null destination account")
    void rejectsNullDestination() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate("ACC001", null, 100.00)
        );
        assertTrue(ex.getMessage().toLowerCase().contains("destination"));
    }

    @Test
    @DisplayName("rejects an empty destination account")
    void rejectsEmptyDestination() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate("ACC001", "", 100.00)
        );
    }

    @Test
    @DisplayName("rejects a transfer to the same account")
    void rejectsSelfTransfer() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate("ACC001", "ACC001", 100.00)
        );
        assertTrue(ex.getMessage().contains("same account"));
    }

    @Test
    @DisplayName("rejects an amount below the minimum transfer ($0.01)")
    void rejectsBelowMinimum() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate("ACC001", "ACC002", 0.0)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate("ACC001", "ACC002", 0.005)
        );
    }

    @Test
    @DisplayName("rejects an amount above the per-transfer maximum ($25,000)")
    void rejectsAboveMaximum() {
        assertThrows(
            IllegalArgumentException.class,
            () -> validator.validate("ACC001", "ACC002", 25_000.01)
        );
    }

    @Test
    @DisplayName("isWithinLimits reports correctly across the boundary cases")
    void isWithinLimitsBoundaries() {
        assertTrue(validator.isWithinLimits(0.01));
        assertTrue(validator.isWithinLimits(25_000.00));
        assertTrue(validator.isWithinLimits(12_345.67));

        assertFalse(validator.isWithinLimits(0.0));
        assertFalse(validator.isWithinLimits(25_000.01));
        assertFalse(validator.isWithinLimits(-1.00));
    }
}
