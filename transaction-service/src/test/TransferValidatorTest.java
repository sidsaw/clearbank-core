import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TransferValidatorTest {

    private static final long ONE_HOUR_MS = 60L * 60L * 1000L;
    private static final long ONE_DAY_MS = 24L * ONE_HOUR_MS;

    private MutableClock clock;
    private TransferValidator validator;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        validator = new TransferValidator(50_000.00, clock);
    }

    // --- Existing per-transfer behaviour (unchanged) -----------------------

    @Test
    void rejectsNullSourceAccount() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate(null, "ACC002", 100.00));
        assertTrue(ex.getMessage().contains("Source account"));
    }

    @Test
    void rejectsEmptySourceAccount() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("", "ACC002", 100.00));
    }

    @Test
    void rejectsNullDestinationAccount() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate("ACC001", null, 100.00));
        assertTrue(ex.getMessage().contains("Destination account"));
    }

    @Test
    void rejectsEmptyDestinationAccount() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("ACC001", "", 100.00));
    }

    @Test
    void rejectsSameAccountTransfer() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate("ACC001", "ACC001", 100.00));
        assertTrue(ex.getMessage().contains("same account"));
    }

    @Test
    void rejectsAmountBelowMinimum() {
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("ACC001", "ACC002", 0.001));
    }

    @Test
    void rejectsAmountExceedingPerTransferCap() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate("ACC001", "ACC002", 25_000.01));
        assertTrue(ex.getMessage().contains("single-transfer limit"));
    }

    @Test
    void acceptsTransferAtPerTransferCap() {
        assertDoesNotThrow(
                () -> validator.validate("ACC001", "ACC002", 25_000.00));
    }

    @Test
    void perTransferCapMessageDoesNotMentionDailyLimit() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate("ACC001", "ACC002", 30_000.00));
        assertTrue(ex.getMessage().contains("single-transfer limit"));
        assertFalse(ex.getMessage().toLowerCase().contains("daily"));
    }

    @Test
    void isWithinLimitsRespectsPerTransferCap() {
        assertTrue(validator.isWithinLimits(25_000.00));
        assertTrue(validator.isWithinLimits(0.01));
        assertFalse(validator.isWithinLimits(25_000.01));
        assertFalse(validator.isWithinLimits(0.0));
    }

    // --- Daily aggregate limit ---------------------------------------------

    @Test
    void rejectsTransferThatWouldExceedDailyLimit() {
        validator.validate("ACC001", "ACC002", 25_000.00);
        validator.validate("ACC001", "ACC002", 24_999.99);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate("ACC001", "ACC002", 1.00));
        assertTrue(ex.getMessage().toLowerCase().contains("daily"));
        assertTrue(ex.getMessage().contains("ACC001"));
    }

    @Test
    void acceptsTransfersExactlyAtDailyLimit() {
        validator.validate("ACC001", "ACC002", 25_000.00);
        assertDoesNotThrow(
                () -> validator.validate("ACC001", "ACC002", 25_000.00));
        assertEquals(50_000.00, validator.getDailyTotal("ACC001"), 0.001);
    }

    @Test
    void rejectsAmountStrictlyAboveDailyLimit() {
        validator.validate("ACC001", "ACC002", 25_000.00);
        validator.validate("ACC001", "ACC002", 25_000.00);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("ACC001", "ACC002", 0.01));
    }

    @Test
    void rejectedTransferIsNotAddedToRunningTotal() {
        validator.validate("ACC001", "ACC002", 25_000.00);
        validator.validate("ACC001", "ACC002", 24_000.00);
        assertEquals(49_000.00, validator.getDailyTotal("ACC001"), 0.001);

        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("ACC001", "ACC002", 2_000.00));

        assertEquals(49_000.00, validator.getDailyTotal("ACC001"), 0.001);
    }

    @Test
    void perTransferCapTakesPrecedenceOverDailyLimit() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> validator.validate("ACC001", "ACC002", 30_000.00));
        assertTrue(ex.getMessage().contains("single-transfer limit"));
        assertEquals(0.0, validator.getDailyTotal("ACC001"), 0.001);
    }

    @Test
    void dailyLimitsAreTrackedPerSourceAccount() {
        validator.validate("ACC001", "ACC003", 25_000.00);
        validator.validate("ACC001", "ACC003", 24_000.00);

        assertDoesNotThrow(
                () -> validator.validate("ACC002", "ACC003", 25_000.00));
        assertDoesNotThrow(
                () -> validator.validate("ACC002", "ACC003", 25_000.00));

        assertEquals(49_000.00, validator.getDailyTotal("ACC001"), 0.001);
        assertEquals(50_000.00, validator.getDailyTotal("ACC002"), 0.001);
    }

    // --- Configurable daily limit ------------------------------------------

    @Test
    void dailyLimitIsConstructorInjectable() {
        TransferValidator strict = new TransferValidator(10_000.00, clock);
        strict.validate("ACC001", "ACC002", 5_000.00);
        strict.validate("ACC001", "ACC002", 4_999.99);
        assertThrows(IllegalArgumentException.class,
                () -> strict.validate("ACC001", "ACC002", 1.00));
    }

    @Test
    void getMaxDailyTransferReflectsInjectedLimit() {
        assertEquals(50_000.00, validator.getMaxDailyTransfer(), 0.001);
        assertEquals(10_000.00,
                new TransferValidator(10_000.00, clock).getMaxDailyTransfer(),
                0.001);
    }

    @Test
    void defaultConstructorUsesFiftyThousandLimit() {
        TransferValidator defaultValidator = new TransferValidator();
        assertEquals(50_000.00, defaultValidator.getMaxDailyTransfer(), 0.001);
    }

    @Test
    void rejectsZeroOrNegativeDailyLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> new TransferValidator(0.0, clock));
        assertThrows(IllegalArgumentException.class,
                () -> new TransferValidator(-1.0, clock));
    }

    // --- Rolling window behaviour ------------------------------------------

    @Test
    void windowResetsAfterTwentyFourHours() {
        validator.validate("ACC001", "ACC002", 25_000.00);
        validator.validate("ACC001", "ACC002", 25_000.00);
        assertEquals(50_000.00, validator.getDailyTotal("ACC001"), 0.001);

        clock.advance(ONE_DAY_MS);

        assertEquals(0.0, validator.getDailyTotal("ACC001"), 0.001);
        assertDoesNotThrow(
                () -> validator.validate("ACC001", "ACC002", 25_000.00));
        assertEquals(25_000.00, validator.getDailyTotal("ACC001"), 0.001);
    }

    @Test
    void windowDoesNotResetBeforeTwentyFourHours() {
        validator.validate("ACC001", "ACC002", 25_000.00);
        validator.validate("ACC001", "ACC002", 25_000.00);

        clock.advance(23 * ONE_HOUR_MS);

        assertEquals(50_000.00, validator.getDailyTotal("ACC001"), 0.001);
        assertThrows(IllegalArgumentException.class,
                () -> validator.validate("ACC001", "ACC002", 0.01));
    }

    @Test
    void windowResetCarriesOnlyTheNewTransfer() {
        validator.validate("ACC001", "ACC002", 20_000.00);
        clock.advance(ONE_DAY_MS + ONE_HOUR_MS);

        validator.validate("ACC001", "ACC002", 15_000.00);
        assertEquals(15_000.00, validator.getDailyTotal("ACC001"), 0.001);
    }

    @Test
    void windowStartIsAnchoredToFirstTransferNotEachOne() {
        validator.validate("ACC001", "ACC002", 20_000.00);
        clock.advance(20 * ONE_HOUR_MS);
        validator.validate("ACC001", "ACC002", 20_000.00);

        assertEquals(40_000.00, validator.getDailyTotal("ACC001"), 0.001);

        clock.advance(5 * ONE_HOUR_MS);

        assertEquals(0.0, validator.getDailyTotal("ACC001"), 0.001);
    }

    // --- getDailyTotal -----------------------------------------------------

    @Test
    void getDailyTotalReturnsZeroForUnknownAccount() {
        assertEquals(0.0, validator.getDailyTotal("ACC999"), 0.001);
    }

    @Test
    void getDailyTotalReflectsRunningSum() {
        assertEquals(0.0, validator.getDailyTotal("ACC001"), 0.001);
        validator.validate("ACC001", "ACC002", 1_000.00);
        assertEquals(1_000.00, validator.getDailyTotal("ACC001"), 0.001);
        validator.validate("ACC001", "ACC002", 250.50);
        assertEquals(1_250.50, validator.getDailyTotal("ACC001"), 0.001);
    }

    // --- Test helpers ------------------------------------------------------

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant initial) {
            this.now = initial;
        }

        void advance(long millis) {
            this.now = this.now.plusMillis(millis);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public long millis() {
            return now.toEpochMilli();
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
