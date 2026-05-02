public class TransferValidator {

    private static final double MIN_TRANSFER = 0.01;
    private static final double MAX_SINGLE_TRANSFER = 25_000.00;

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
    }

    public boolean isWithinLimits(double amount) {
        return amount >= MIN_TRANSFER && amount <= MAX_SINGLE_TRANSFER;
    }
}
