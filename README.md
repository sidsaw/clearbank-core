# ClearBank Core

**ClearBank** is a consumer digital banking platform built to power the everyday financial lives of its customers — much like Bank of America's digital banking experience. From checking and savings account management to real-time money movement, ClearBank gives users secure, 24/7 access to their finances through a modern, API-driven architecture.

## What ClearBank Does

ClearBank provides the backend services that drive a full-featured digital banking application:

- **Authentication & Security** — Secure login, session management, and token-based access control protect every customer interaction, ensuring accounts are accessible only to verified users.
- **Money Movement** — Customers can make deposits, withdrawals, and account-to-account transfers instantly. The platform is designed to support the same categories of transactions found in major consumer banking apps: internal transfers, balance inquiries, and real-time transaction history.
- **PII Protection & Compliance** — Personally identifiable information (SSNs, account numbers, emails) is automatically masked and validated before it reaches downstream systems, keeping ClearBank aligned with financial data privacy regulations.
- **Audit & Monitoring** — Every significant action — logins, transfers, account changes — is recorded in a tamper-evident audit trail. Events can be filtered by user or type, supporting both internal compliance reviews and regulatory reporting.

## Platform Overview

ClearBank is structured as a **microservices monorepo**. Each service owns a single domain and communicates through well-defined interfaces, mirroring the architecture patterns used by large-scale digital banking platforms.

## Services

| Service | Language | Description |
|---------|----------|-------------|
| [auth-service](auth-service/) | TypeScript | Authentication, login, token validation, and session management |
| [transaction-service](transaction-service/) | Java | Account deposits, withdrawals, transfers, and balance lookups |
| [pii-service](pii-service/) | Python | PII masking (SSN, account numbers), email validation, and record redaction |
| [audit-service](audit-service/) | TypeScript | Event logging, audit trail, and filtering by user or event type |

## Architecture

See [docs/architecture.pdf](docs/architecture.pdf) for the full system diagram.

```
┌──────────────┐     ┌────────────────────┐     ┌──────────────┐
│  auth-service │────▶│ transaction-service │────▶│ audit-service │
│  (TypeScript) │     │       (Java)        │     │ (TypeScript)  │
└──────────────┘     └────────────────────┘     └──────────────┘
                              │
                              ▼
                      ┌──────────────┐
                      │  pii-service  │
                      │   (Python)    │
                      └──────────────┘
```

## Coverage

Run `python coverage_report.py` from the repository root to generate a coverage summary across all services.
