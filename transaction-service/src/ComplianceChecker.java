import java.util.ArrayList;
import java.util.List;

/**
 * Enforces regulatory compliance rules for financial transactions.
 * Covers AML thresholds, daily limits, and suspicious-activity detection.
 */
public class ComplianceChecker {

    private static final double CTR_THRESHOLD = 10_000.00;
    private static final double DAILY_LIMIT = 50_000.00;
    private static final double STRUCTURING_WINDOW_AMOUNT = 9_000.00;

    public ComplianceResult check(String accountId, double amount,
                                   double dailyTotal, List<Double> recentAmounts) {
        List<String> violations = new ArrayList<>();

        if (amount >= CTR_THRESHOLD) {
            violations.add("CTR_FILING_REQUIRED");
        }

        if (dailyTotal + amount > DAILY_LIMIT) {
            violations.add("DAILY_LIMIT_EXCEEDED");
        }

        if (detectStructuring(recentAmounts, amount)) {
            violations.add("STRUCTURING_SUSPECTED");
        }

        boolean blocked = violations.contains("DAILY_LIMIT_EXCEEDED")
                       || violations.contains("STRUCTURING_SUSPECTED");

        return new ComplianceResult(violations, blocked);
    }

    private boolean detectStructuring(List<Double> recentAmounts, double current) {
        int suspiciousCount = 0;
        for (double amt : recentAmounts) {
            if (amt >= STRUCTURING_WINDOW_AMOUNT && amt < CTR_THRESHOLD) {
                suspiciousCount++;
            }
        }
        if (current >= STRUCTURING_WINDOW_AMOUNT && current < CTR_THRESHOLD) {
            suspiciousCount++;
        }
        return suspiciousCount >= 3;
    }

    public static class ComplianceResult {
        public final List<String> violations;
        public final boolean blocked;

        public ComplianceResult(List<String> violations, boolean blocked) {
            this.violations = violations;
            this.blocked = blocked;
        }
    }
}
