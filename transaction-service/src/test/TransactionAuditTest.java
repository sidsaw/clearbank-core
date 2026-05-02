import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionAuditTest {

    private TransactionAudit audit;

    @BeforeEach
    void setUp() {
        audit = new TransactionAudit();
    }

    @Test
    @DisplayName("records a transaction with the supplied fields")
    void recordsTransaction() {
        audit.recordTransaction("TX1", "ACC001", "DEPOSIT", 100.00, "SUCCESS");

        List<TransactionAudit.AuditRecord> records = audit.getRecordsForAccount("ACC001");

        assertEquals(1, records.size());
        TransactionAudit.AuditRecord record = records.get(0);
        assertEquals("TX1", record.transactionId);
        assertEquals("ACC001", record.accountId);
        assertEquals("DEPOSIT", record.type);
        assertEquals(100.00, record.amount, 0.0001);
        assertEquals("SUCCESS", record.status);
    }

    @Test
    @DisplayName("filters records by account id")
    void filtersByAccount() {
        audit.recordTransaction("TX1", "ACC001", "DEPOSIT", 100.00, "SUCCESS");
        audit.recordTransaction("TX2", "ACC002", "DEPOSIT", 200.00, "SUCCESS");
        audit.recordTransaction("TX3", "ACC001", "WITHDRAW", 50.00, "SUCCESS");

        List<TransactionAudit.AuditRecord> acc1 = audit.getRecordsForAccount("ACC001");
        List<TransactionAudit.AuditRecord> acc2 = audit.getRecordsForAccount("ACC002");

        assertEquals(2, acc1.size());
        assertEquals(1, acc2.size());
    }

    @Test
    @DisplayName("returns failed and blocked transactions in getFailedTransactions")
    void returnsFailedAndBlocked() {
        audit.recordTransaction("TX1", "ACC001", "DEPOSIT", 100.00, "SUCCESS");
        audit.recordTransaction("TX2", "ACC002", "DEPOSIT", 200.00, "FAILED");
        audit.recordTransaction("TX3", "ACC003", "TRANSFER", 50.00, "BLOCKED");

        List<TransactionAudit.AuditRecord> failed = audit.getFailedTransactions();

        assertEquals(2, failed.size());
        assertTrue(failed.stream().anyMatch(r -> "TX2".equals(r.transactionId)));
        assertTrue(failed.stream().anyMatch(r -> "TX3".equals(r.transactionId)));
    }

    @Test
    @DisplayName("getRecordsForAccount returns an unmodifiable list")
    void recordsAreUnmodifiable() {
        audit.recordTransaction("TX1", "ACC001", "DEPOSIT", 100.00, "SUCCESS");
        List<TransactionAudit.AuditRecord> records = audit.getRecordsForAccount("ACC001");

        assertThrows(UnsupportedOperationException.class,
            () -> records.add(null));
    }

    @Test
    @DisplayName("verifyIntegrity returns true when every record has a transaction and account id")
    void verifyIntegrityReturnsTrueWhenWellFormed() {
        audit.recordTransaction("TX1", "ACC001", "DEPOSIT", 100.00, "SUCCESS");
        audit.recordTransaction("TX2", "ACC002", "WITHDRAW", 50.00, "SUCCESS");

        assertTrue(audit.verifyIntegrity());
    }

    @Test
    @DisplayName("verifyIntegrity returns false when a record is missing its transaction id")
    void verifyIntegrityCatchesMissingTransactionId() {
        audit.recordTransaction(null, "ACC001", "DEPOSIT", 100.00, "SUCCESS");

        assertFalse(audit.verifyIntegrity());
    }
}
