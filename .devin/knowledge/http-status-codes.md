---
description: Guidelines for which HTTP status codes to use in ClearBank Core API responses. Reference this when writing or reviewing API endpoint logic, error handling, or tests that assert on response codes.
---

# HTTP Status Code Standards

These standards apply to every HTTP-facing service in ClearBank Core (`auth-service`, `transaction-service`, `pii-service`, `audit-service`). Pick the most specific code that accurately describes the outcome — never use a generic 200/500 to paper over a more meaningful state.

## Success Codes (2xx)

| Code | Name       | When to Use                                                                                                                                                  |
| ---- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 200  | OK         | Successful `GET`, or successful `POST`/`PUT`/`PATCH` that returns a body (e.g. account balance lookup, successful login that returns a session token).        |
| 201  | Created    | A new resource was created and persisted — e.g. a new user account, a new transaction record, a new audit entry. Include the resource's canonical ID in the body and a `Location` header where applicable. |
| 204  | No Content | A successful mutation that intentionally returns no body — e.g. logout, delete, PII redaction acknowledgement, or a successful idempotent retry that has nothing new to report. |

## Client Error Codes (4xx)

| Code | Name                | When to Use                                                                                                                                                                                  |
| ---- | ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 400  | Bad Request         | The request is syntactically malformed: invalid JSON, missing required field, wrong type, schema violation. Use this for *shape* problems, not business-rule problems.                       |
| 401  | Unauthorized        | The caller is not authenticated — missing, expired, or invalid session token / API key. The client can retry after authenticating.                                                            |
| 403  | Forbidden           | The caller is authenticated but lacks permission for this resource or action — e.g. a customer trying to read another customer's account, or a non-admin hitting an admin-only audit export. |
| 404  | Not Found           | The requested resource does not exist (or the caller is not allowed to know it exists). Use for unknown account IDs, unknown transaction IDs, unknown audit records.                          |
| 409  | Conflict            | The request collides with current resource state — e.g. duplicate transaction idempotency key, attempting to create an account with an email that already exists, optimistic-lock version mismatch on a ledger update. |
| 422  | Unprocessable Entity| The request is well-formed but violates a business rule — e.g. withdrawal would overdraft the account, transfer amount below minimum, PII field fails validation policy, KYC checks failed. |

## Server Error Codes (5xx)

| Code | Name                | When to Use                                                                                                                                                          |
| ---- | ------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 500  | Internal Server Error | An unexpected, unhandled exception in our own code — bugs, null dereferences, failed invariants. These should always be logged and alerted on.                     |
| 502  | Bad Gateway         | An upstream dependency we proxy to returned an invalid or unparseable response — e.g. the core ledger backend returned malformed data, or a downstream KYC vendor returned garbage. |
| 503  | Service Unavailable | The service or a critical dependency is temporarily unable to handle the request — e.g. database connection pool exhausted, circuit breaker open, scheduled maintenance, queue saturated. Include a `Retry-After` header where possible. |

## Rules

1. **Never return `200 OK` for an error.** Every error path must return a 4xx or 5xx. Do not embed `{"error": "..."}` inside a 200 response.
2. **Prefer `422 Unprocessable Entity` over `400 Bad Request` for business-rule validation failures.** Reserve 400 for *syntactic* / schema-level problems; use 422 when the request was understood but rejected by domain logic (insufficient funds, account frozen, transfer limits exceeded, PII policy violation, etc.).
3. **Use `401` vs `403` correctly.** `401 Unauthorized` means "I don't know who you are" — the caller is unauthenticated or their credentials are invalid/expired. `403 Forbidden` means "I know who you are, but you can't do this" — the caller is authenticated but not permitted. Never use 403 for unauthenticated callers, and never use 401 when the caller is authenticated but lacks permission.
4. **All error responses MUST include a JSON body.** No empty error responses, no `text/plain`, no HTML error pages. The body must be valid JSON with at minimum a stable machine-readable code and a human-readable message, e.g.:
   ```json
   {
     "code": "INSUFFICIENT_FUNDS",
     "message": "Withdrawal of $500.00 would overdraft account ****1234.",
     "requestId": "req_01HZX..."
   }
   ```
   Tests asserting on error responses should assert on both the status code and the body's `code` field.
