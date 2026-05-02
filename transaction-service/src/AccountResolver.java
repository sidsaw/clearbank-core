import java.util.HashMap;
import java.util.Map;

public class AccountResolver {

    private final Map<String, AccountInfo> accounts = new HashMap<>();

    public AccountResolver() {
        accounts.put("ACC001", new AccountInfo("ACC001", "Alice Johnson", "CHECKING", true));
        accounts.put("ACC002", new AccountInfo("ACC002", "Bob Smith", "SAVINGS", true));
        accounts.put("ACC003", new AccountInfo("ACC003", "Carol Davis", "CHECKING", true));
    }

    public AccountInfo resolve(String accountId) {
        AccountInfo info = accounts.get(accountId);
        if (info == null) {
            throw new IllegalArgumentException("Unknown account: " + accountId);
        }
        return info;
    }

    public boolean isActive(String accountId) {
        AccountInfo info = accounts.get(accountId);
        return info != null && info.active;
    }

    public String getAccountType(String accountId) {
        AccountInfo info = accounts.get(accountId);
        return info != null ? info.type : null;
    }

    public static class AccountInfo {
        public final String id;
        public final String ownerName;
        public final String type;
        public final boolean active;

        public AccountInfo(String id, String ownerName, String type, boolean active) {
            this.id = id;
            this.ownerName = ownerName;
            this.type = type;
            this.active = active;
        }
    }
}
