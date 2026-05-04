import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

public class TransferValidator {

    private static final double MIN_TRANSFER = 0.01;
    private static final double MAX_SINGLE_TRANSFER = 25_000.00;
    private static final double DEFAULT_MAX_DAILY_TRANSFER = 50_000.00;
    private static final long ROLLING_WINDOW_MS = 24L * 60L * 60L * 1000L;

    private final double maxDailyTransfer;
    private final Clock clock;
    private final Map<String, DailyTotal> dailyTotals = new HashMap<>();

    public TransferValidator() {
        this(DEFAULT_MAX_DAILY_TRANSFER, Clock.systemUTC());
    }

    public TransferValidator(double maxDailyTransfer) {
        this(maxDailyTransfer, Clock.systemUTC());
    }

    public TransferValidator(double maxDailyTransfer, Clock clock) {
        if (maxDailyTransfer <= 0) {
            throw new IllegalArgumentException("Daily transfer limit must be positive");
        }
        if (clock == null) {
            throw new IllegalArgumentException("Clock is required");
        }
        this.maxDailyTransfer = maxDailyTransfer;
        this.clock = clock;
    }

    public void validate(String fromAccount, String toAccount, double amount) {
        if (fromAccount == null || fromAccount.isEmpty()) {
            throw new IllegalArgumentException("Source account is required");
        }
        if (toAccount == null || toAccount.isEmpty()) {
            throw new IllegalArgumentException("Destination account is required");
        }
        if (fromAccount.equals(toAccount)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        if (amount < MIN_TRANSFER) {
            throw new IllegalArgumentException(
                    "Transfer amount must be at least " + MIN_TRANSFER);
        }
        if (amount > MAX_SINGLE_TRANSFER) {
            throw new IllegalArgumentException(
                    "Transfer amount exceeds single-transfer limit of " + MAX_SINGLE_TRANSFER);
        }

        double currentDaily = getDailyTotal(fromAccount);
        if (currentDaily + amount > maxDailyTransfer) {
            throw new IllegalArgumentException(
                    "Transfer would exceed daily transfer limit of " + maxDailyTransfer
                            + " for account " + fromAccount);
        }

        recordTransfer(fromAccount, amount);
    }

    public boolean isWithinLimits(double amount) {
        return amount >= MIN_TRANSFER && amount <= MAX_SINGLE_TRANSFER;
    }

    public double getDailyTotal(String accountId) {
        DailyTotal total = dailyTotals.get(accountId);
        if (total == null) {
            return 0.0;
        }
        if (clock.millis() - total.windowStart >= ROLLING_WINDOW_MS) {
            return 0.0;
        }
        return total.amount;
    }

    public double getMaxDailyTransfer() {
        return maxDailyTransfer;
    }

    private void recordTransfer(String accountId, double amount) {
        long now = clock.millis();
        DailyTotal existing = dailyTotals.get(accountId);
        if (existing == null || now - existing.windowStart >= ROLLING_WINDOW_MS) {
            dailyTotals.put(accountId, new DailyTotal(amount, now));
        } else {
            dailyTotals.put(accountId,
                    new DailyTotal(existing.amount + amount, existing.windowStart));
        }
    }

    private static final class DailyTotal {
        final double amount;
        final long windowStart;

        DailyTotal(double amount, long windowStart) {
            this.amount = amount;
            this.windowStart = windowStart;
        }
    }
}
