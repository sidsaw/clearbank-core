import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComplianceCheckerTest {

    private static final String ACC = "ACC001";

    private ComplianceChecker checker;

    @BeforeEach
    void setUp() {
        checker = new ComplianceChecker();
    }

    @Nested
    @DisplayName("CTR filing threshold ($10,000)")
    class CtrThreshold {

        @Test
        @DisplayName("flags but does not block a transaction at or above $10,000")
        void flagsAtThreshold() {
            ComplianceChecker.ComplianceResult result =
                checker.check(ACC, 10_000.00, 0.0, Collections.emptyList());

            assertTrue(result.violations.contains("CTR_FILING_REQUIRED"));
            assertFalse(result.blocked, "CTR filing is a reporting requirement, not a hard block");
        }

        @Test
        @DisplayName("does not flag a sub-threshold transaction")
        void doesNotFlagBelowThreshold() {
            ComplianceChecker.ComplianceResult result =
                checker.check(ACC, 9_999.99, 0.0, Collections.emptyList());

            assertFalse(result.violations.contains("CTR_FILING_REQUIRED"));
        }
    }

    @Nested
    @DisplayName("daily limit ($50,000)")
    class DailyLimit {

        @Test
        @DisplayName("blocks a transaction that pushes the daily total over $50,000")
        void blocksOverDailyLimit() {
            ComplianceChecker.ComplianceResult result =
                checker.check(ACC, 5_000.00, 49_000.00, Collections.emptyList());

            assertTrue(result.violations.contains("DAILY_LIMIT_EXCEEDED"));
            assertTrue(result.blocked, "Daily-limit breach is a hard block");
        }

        @Test
        @DisplayName("permits a transaction exactly at the daily limit")
        void allowsAtDailyLimit() {
            ComplianceChecker.ComplianceResult result =
                checker.check(ACC, 1_000.00, 49_000.00, Collections.emptyList());

            assertFalse(result.violations.contains("DAILY_LIMIT_EXCEEDED"));
            assertFalse(result.blocked);
        }
    }

    @Nested
    @DisplayName("structuring detection ($9,000-$9,999.99 window)")
    class Structuring {

        @Test
        @DisplayName("flags and blocks once the third in-window transaction occurs")
        void blocksOnThirdSuspiciousTransaction() {
            List<Double> recent = Arrays.asList(9_500.00, 9_200.00);

            ComplianceChecker.ComplianceResult result =
                checker.check(ACC, 9_300.00, 0.0, recent);

            assertTrue(result.violations.contains("STRUCTURING_SUSPECTED"));
            assertTrue(result.blocked, "Structuring is a hard block");
        }

        @Test
        @DisplayName("does not flag two in-window transactions on their own")
        void doesNotFlagBelowThreshold() {
            List<Double> recent = Arrays.asList(9_500.00);

            ComplianceChecker.ComplianceResult result =
                checker.check(ACC, 9_200.00, 0.0, recent);

            assertFalse(result.violations.contains("STRUCTURING_SUSPECTED"));
        }

        @Test
        @DisplayName("ignores amounts at or above the CTR threshold (those file CTRs instead)")
        void ignoresCtrSizedTransactions() {
            List<Double> recent = Arrays.asList(10_000.00, 10_500.00, 11_000.00);

            ComplianceChecker.ComplianceResult result =
                checker.check(ACC, 10_200.00, 0.0, recent);

            assertFalse(result.violations.contains("STRUCTURING_SUSPECTED"));
        }
    }

    @Test
    @DisplayName("a clean transaction has no violations and is not blocked")
    void cleanTransaction() {
        ComplianceChecker.ComplianceResult result =
            checker.check(ACC, 100.00, 500.00, Collections.emptyList());

        assertEquals(0, result.violations.size());
        assertFalse(result.blocked);
    }
}
