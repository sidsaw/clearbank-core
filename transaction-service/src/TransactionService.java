import java.util.HashMap;
import java.util.Map;

public class TransactionService {

    private final Map<String, Double> accounts = new HashMap<>();

    public TransactionService() {
        accounts.put("ACC001", 1000.00);
        accounts.put("ACC002", 500.00);
        accounts.put("ACC003", 2500.00);
    }

    public double deposit(String accountId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        double current = accounts.getOrDefault(accountId, 0.0);
        double updated = current + amount;
        accounts.put(accountId, updated);
        return updated;
    }

    public double withdraw(String accountId, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        double current = accounts.getOrDefault(accountId, 0.0);
        if (amount > current) {
            throw new IllegalStateException("Insufficient funds for account " + accountId);
        }
        double updated = current - amount;
        accounts.put(accountId, updated);
        return updated;
    }

    public boolean transfer(String fromId, String toId, double amount) {
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        withdraw(fromId, amount);
        deposit(toId, amount);
        return true;
    }

    public double getBalance(String accountId) {
        if (!accounts.containsKey(accountId)) {
            throw new IllegalArgumentException("Account not found: " + accountId);
        }
        return accounts.get(accountId);
    }
}
