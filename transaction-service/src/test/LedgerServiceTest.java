import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LedgerServiceTest {

    private static final double DELTA = 0.0001;

    private LedgerService ledger;

    @BeforeEach
    void setUp() {
        ledger = new LedgerService();
    }

    @Test
    @DisplayName("computeBalance is zero for an account with no entries")
    void zeroBalanceForUnseenAccount() {
        assertEquals(0.0, ledger.computeBalance("ACC001"), DELTA);
    }

    @Test
    @DisplayName("CREDIT entries increase and DEBIT entries decrease the balance")
    void creditAndDebitMath() {
        ledger.record("ACC001", "CREDIT", 1000.00, "deposit");
        ledger.record("ACC001", "DEBIT", 200.00, "atm");
        ledger.record("ACC001", "CREDIT", 50.00, "refund");

        assertEquals(850.00, ledger.computeBalance("ACC001"), DELTA);
    }

    @Test
    @DisplayName("entries from other accounts do not affect this account's balance")
    void balancesAreScopedPerAccount() {
        ledger.record("ACC001", "CREDIT", 1000.00, "deposit");
        ledger.record("ACC002", "CREDIT", 999.00, "deposit");

        assertEquals(1000.00, ledger.computeBalance("ACC001"), DELTA);
        assertEquals(999.00, ledger.computeBalance("ACC002"), DELTA);
    }

    @Test
    @DisplayName("unknown entry types do not change the balance")
    void unknownTypeIsIgnored() {
        ledger.record("ACC001", "FEE", 25.00, "service fee");

        assertEquals(0.0, ledger.computeBalance("ACC001"), DELTA);
    }

    @Test
    @DisplayName("getEntriesForAccount returns only this account's entries, in insertion order")
    void filtersAndPreservesOrder() {
        ledger.record("ACC001", "CREDIT", 100.00, "first");
        ledger.record("ACC002", "CREDIT", 200.00, "ignored");
        ledger.record("ACC001", "DEBIT", 50.00, "second");

        List<LedgerService.LedgerEntry> entries = ledger.getEntriesForAccount("ACC001");

        assertEquals(2, entries.size());
        assertEquals("first", entries.get(0).description);
        assertEquals("second", entries.get(1).description);
    }

    @Test
    @DisplayName("getEntriesForAccount returns an unmodifiable list")
    void entriesAreUnmodifiable() {
        ledger.record("ACC001", "CREDIT", 100.00, "deposit");
        List<LedgerService.LedgerEntry> entries = ledger.getEntriesForAccount("ACC001");

        assertThrows(UnsupportedOperationException.class,
            () -> entries.add(null));
    }

    @Test
    @DisplayName("totalEntryCount counts entries across all accounts")
    void countsAcrossAccounts() {
        ledger.record("ACC001", "CREDIT", 100.00, "");
        ledger.record("ACC002", "CREDIT", 200.00, "");
        ledger.record("ACC003", "DEBIT", 50.00, "");

        assertEquals(3, ledger.totalEntryCount());
    }

    @Test
    @DisplayName("rejects a null or empty account id")
    void rejectsBadAccountId() {
        assertThrows(IllegalArgumentException.class,
            () -> ledger.record(null, "CREDIT", 100.00, ""));
        assertThrows(IllegalArgumentException.class,
            () -> ledger.record("", "CREDIT", 100.00, ""));
    }
}
