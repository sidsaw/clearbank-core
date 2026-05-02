import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Records an immutable audit trail of all financial transactions.
 * Required by OCC for regulatory examination and dispute resolution.
 */
public class TransactionAudit {

    private final List<AuditRecord> records = new ArrayList<>();

    public void recordTransaction(String transactionId, String accountId,
                                   String type, double amount, String status) {
        records.add(new AuditRecord(
                transactionId, accountId, type, amount, status, Instant.now()));
    }

    public List<AuditRecord> getRecordsForAccount(String accountId) {
        List<AuditRecord> result = new ArrayList<>();
        for (AuditRecord record : records) {
            if (record.accountId.equals(accountId)) {
                result.add(record);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public List<AuditRecord> getFailedTransactions() {
        List<AuditRecord> result = new ArrayList<>();
        for (AuditRecord record : records) {
            if ("FAILED".equals(record.status) || "BLOCKED".equals(record.status)) {
                result.add(record);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public boolean verifyIntegrity() {
        for (AuditRecord record : records) {
            if (record.transactionId == null || record.accountId == null) {
                return false;
            }
        }
        return true;
    }

    public static class AuditRecord {
        public final String transactionId;
        public final String accountId;
        public final String type;
        public final double amount;
        public final String status;
        public final Instant timestamp;

        public AuditRecord(String transactionId, String accountId, String type,
                           double amount, String status, Instant timestamp) {
            this.transactionId = transactionId;
            this.accountId = accountId;
            this.type = type;
            this.amount = amount;
            this.status = status;
            this.timestamp = timestamp;
        }
    }
}
