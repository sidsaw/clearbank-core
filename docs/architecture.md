# ClearBank Core — Architecture

## System Overview

```mermaid
graph TB
    subgraph Clients
        WEB[Web App]
        MOB[Mobile App]
    end

    subgraph API Gateway
        GW[API Gateway / Router]
    end

    subgraph auth-service [Auth Service — Java]
        AUTH_LOGIN[login]
        AUTH_VALIDATE[validateToken]
        AUTH_LOGOUT[logout]
        AUTH_STORE[(User Credentials Store)]
        AUTH_LOGIN --> AUTH_STORE
        AUTH_VALIDATE --> AUTH_STORE
    end

    subgraph transaction-service [Transaction Service — Java]
        TXN_DEPOSIT[deposit]
        TXN_WITHDRAW[withdraw]
        TXN_TRANSFER[transfer]
        TXN_BALANCE[getBalance]
        TXN_STORE[(In-Memory Account Ledger)]
        TXN_DEPOSIT --> TXN_STORE
        TXN_WITHDRAW --> TXN_STORE
        TXN_TRANSFER --> TXN_WITHDRAW
        TXN_TRANSFER --> TXN_DEPOSIT
        TXN_BALANCE --> TXN_STORE
    end

    subgraph pii-service [PII Service — Python]
        PII_SSN[mask_ssn]
        PII_ACCT[mask_account]
        PII_EMAIL[is_valid_email]
        PII_REDACT[redact_record]
        PII_REDACT --> PII_SSN
        PII_REDACT --> PII_ACCT
        PII_REDACT --> PII_EMAIL
    end

    subgraph audit-service [Audit Service — TypeScript]
        AUDIT_LOG[logEvent]
        AUDIT_USER[getEvents by userId]
        AUDIT_TYPE[getEventsByType]
        AUDIT_STORE[(In-Memory Event Log)]
        AUDIT_LOG --> AUDIT_STORE
        AUDIT_USER --> AUDIT_STORE
        AUDIT_TYPE --> AUDIT_STORE
    end

    WEB --> GW
    MOB --> GW

    GW -- "authenticate" --> AUTH_LOGIN
    GW -- "verify token" --> AUTH_VALIDATE
    GW -- "end session" --> AUTH_LOGOUT

    GW -- "deposit / withdraw / transfer / balance" --> TXN_DEPOSIT
    GW -- "deposit / withdraw / transfer / balance" --> TXN_WITHDRAW
    GW -- "deposit / withdraw / transfer / balance" --> TXN_TRANSFER
    GW -- "deposit / withdraw / transfer / balance" --> TXN_BALANCE

    TXN_DEPOSIT -. "redact before response" .-> PII_REDACT
    TXN_BALANCE -. "redact before response" .-> PII_REDACT

    AUTH_LOGIN -. "log: LOGIN" .-> AUDIT_LOG
    AUTH_LOGOUT -. "log: LOGOUT" .-> AUDIT_LOG
    TXN_DEPOSIT -. "log: DEPOSIT" .-> AUDIT_LOG
    TXN_WITHDRAW -. "log: WITHDRAWAL" .-> AUDIT_LOG
    TXN_TRANSFER -. "log: TRANSFER" .-> AUDIT_LOG
```

## Service Details

### Auth Service (Java)

Manages user identity and session lifecycle.

| Method | Description |
|--------|-------------|
| `login(username, password)` | Validates credentials against the user store and returns a session token (`valid-<user>-<timestamp>`) |
| `validateToken(token)` | Checks that a token has the expected `valid-` prefix |
| `logout(token)` | Terminates the session associated with the given token |

**Data store:** In-memory `HashMap<String, String>` of username → password.

---

### Transaction Service (Java)

Core financial engine handling all money movement and balance operations.

| Method | Description |
|--------|-------------|
| `deposit(accountId, amount)` | Credits the account and returns the new balance |
| `withdraw(accountId, amount)` | Debits the account (fails on insufficient funds) and returns the new balance |
| `transfer(fromId, toId, amount)` | Atomically withdraws from one account and deposits to another |
| `getBalance(accountId)` | Returns the current balance (fails if account does not exist) |

**Data store:** In-memory `HashMap<String, Double>` of accountId → balance.  
**Seeded accounts:** `ACC001` ($1,000), `ACC002` ($500), `ACC003` ($2,500).

---

### PII Service (Python)

Protects sensitive customer data before it leaves the platform.

| Function | Description |
|----------|-------------|
| `mask_ssn(ssn)` | Replaces all but the last 4 digits of a SSN → `XXX-XX-1234` |
| `mask_account(account_number)` | Replaces all but the last 4 characters → `****5678` |
| `is_valid_email(email)` | Basic format check (`@` and `.` in domain) |
| `redact_record(record)` | Applies SSN masking, account masking, and email validation to a full customer record |

---

### Audit Service (TypeScript)

Immutable event log for compliance and observability.

| Function | Description |
|----------|-------------|
| `logEvent(eventType, userId, details)` | Appends a timestamped event to the log |
| `getEvents(userId)` | Returns all events for a given user |
| `getEventsByType(eventType)` | Returns all events of a given type (e.g., `LOGIN`, `TRANSFER`) |
| `clearEvents()` | Resets the event log (testing only) |

**Event schema:**
```json
{
  "eventType": "TRANSFER",
  "userId": "alice",
  "details": { "from": "ACC001", "to": "ACC002", "amount": 250.00 },
  "timestamp": 1714567890123
}
```

## Data Flow

```mermaid
sequenceDiagram
    actor User
    participant GW as API Gateway
    participant Auth as Auth Service
    participant Txn as Transaction Service
    participant PII as PII Service
    participant Audit as Audit Service

    User->>GW: POST /login (username, password)
    GW->>Auth: login(username, password)
    Auth-->>GW: session token
    Auth--)Audit: logEvent("LOGIN", userId, {})
    GW-->>User: 200 OK + token

    User->>GW: POST /transfer (from, to, amount) + token
    GW->>Auth: validateToken(token)
    Auth-->>GW: true
    GW->>Txn: transfer(from, to, amount)
    Txn->>Txn: withdraw(from, amount)
    Txn->>Txn: deposit(to, amount)
    Txn--)Audit: logEvent("TRANSFER", userId, {from, to, amount})
    Txn->>PII: redact_record(response)
    PII-->>Txn: redacted response
    Txn-->>GW: transfer result
    GW-->>User: 200 OK

    User->>GW: GET /balance (accountId) + token
    GW->>Auth: validateToken(token)
    Auth-->>GW: true
    GW->>Txn: getBalance(accountId)
    Txn->>PII: redact_record(accountInfo)
    PII-->>Txn: masked accountInfo
    Txn-->>GW: balance
    GW-->>User: 200 OK + balance

    User->>GW: POST /logout + token
    GW->>Auth: logout(token)
    Auth--)Audit: logEvent("LOGOUT", userId, {})
    Auth-->>GW: OK
    GW-->>User: 200 OK
```
