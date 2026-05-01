# clearbank-core

Demo monorepo for ClearBank's core services. Each service is intentionally minimal — this repository exists to demonstrate cross-service architecture and test coverage workflows.

## Services

| Service | Description |
|---------|-------------|
| [auth-service](auth-service/) | Authentication, login, and token validation |
| [transaction-service](transaction-service/) | Account deposits, withdrawals, and transfers |
| [pii-service](pii-service/) | PII masking and validation utilities |
| [audit-service](audit-service/) | Event logging and audit trail |

## Architecture

See [docs/architecture.pdf](docs/architecture.pdf) for the system diagram.

## Coverage

Run `python coverage_report.py` from the repository root to generate a coverage summary across all services.
