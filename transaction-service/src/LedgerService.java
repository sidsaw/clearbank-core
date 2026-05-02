import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LedgerService {

    private final List<LedgerEntry> entries = new ArrayList<>();

    public void record(String accountId, String type, double amount, String description) {
        if (accountId == null || accountId.isEmpty()) {
            throw new IllegalArgumentException("Account ID is required");
        }
        entries.add(new LedgerEntry(accountId, type, amount, description, Instant.now()));
    }

    public List<LedgerEntry> getEntriesForAccount(String accountId) {
        List<LedgerEntry> result = new ArrayList<>();
        for (LedgerEntry entry : entries) {
            if (entry.accountId.equals(accountId)) {
                result.add(entry);
            }
        }
        return Collections.unmodifiableList(result);
    }

    public double computeBalance(String accountId) {
        double balance = 0.0;
        for (LedgerEntry entry : entries) {
            if (entry.accountId.equals(accountId)) {
                if ("CREDIT".equals(entry.type)) {
                    balance += entry.amount;
                } else if ("DEBIT".equals(entry.type)) {
                    balance -= entry.amount;
                }
            }
        }
        return balance;
    }

    public int totalEntryCount() {
        return entries.size();
    }

    public static class LedgerEntry {
        public final String accountId;
        public final String type;
        public final double amount;
        public final String description;
        public final Instant timestamp;

        public LedgerEntry(String accountId, String type, double amount,
                           String description, Instant timestamp) {
            this.accountId = accountId;
            this.type = type;
            this.amount = amount;
            this.description = description;
            this.timestamp = timestamp;
        }
    }
}
