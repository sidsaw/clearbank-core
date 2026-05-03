#!/usr/bin/env python3
"""Generate a coverage audit report across services in the monorepo.

Tracks two dimensions per service:
  1. File coverage  — test files vs. source files (rough estimate, as before)
  2. Compliance coverage — which compliance-critical functions have test coverage
"""

import os
from datetime import datetime, timezone
from pathlib import Path

SERVICES = [
    "auth-service",
    "transaction-service",
    "pii-service",
    "audit-service",
]

SOURCE_EXTENSIONS = (".java", ".py", ".ts")

AUDIT_REPORTS_DIR = Path("audit_reports")

# ---------------------------------------------------------------------------
# Compliance-critical functions per service
#
# Each entry maps a service name to a list of (source_file, function_name)
# pairs. The function_name is searched as a plain substring in test file
# content, so it should match the actual identifier used in tests (e.g. the
# method name or the Python/TS function name).
# ---------------------------------------------------------------------------
COMPLIANCE_FUNCTIONS = {
    "auth-service": [
        # Credential / session management
        ("AuthService.java",      "login"),
        ("AuthService.java",      "validateToken"),
        ("AuthService.java",      "logout"),
        ("AuthService.java",      "refreshToken"),
        # Account lockout
        ("AccountLockout.java",   "isLocked"),
        ("AccountLockout.java",   "recordFailure"),
        ("AccountLockout.java",   "resetFailures"),
        ("AccountLockout.java",   "getFailureCount"),
        # Audit logging
        ("AuditLogger.java",      "log"),
        ("AuditLogger.java",      "getEntriesForUser"),
        ("AuditLogger.java",      "getEntriesByType"),
        ("AuditLogger.java",      "clear"),
        # Configuration
        ("AuthConfig.java",       "getSessionTtlMs"),
        ("AuthConfig.java",       "getMaxLoginAttempts"),
        ("AuthConfig.java",       "getLockoutDurationMs"),
        # Permission / rate enforcement
        ("PermissionChecker.java","hasPermission"),
        ("PermissionChecker.java","requirePermission"),
        ("RateLimiter.java",      "checkRate"),
        ("RateLimiter.java",      "isRateLimited"),
        # Password / token / session
        ("PasswordValidator.java","validate"),
        ("SessionManager.java",   "create"),
        ("SessionManager.java",   "isValid"),
        ("SessionManager.java",   "invalidate"),
        ("TokenGenerator.java",   "generate"),
        ("TokenGenerator.java",   "extractUsername"),
        ("UserRepository.java",   "getPassword"),
        ("UserRepository.java",   "exists"),
    ],
    "transaction-service": [
        # Core ledger mutations
        ("TransactionService.java",  "deposit"),
        ("TransactionService.java",  "withdraw"),
        ("TransactionService.java",  "transfer"),
        ("TransactionService.java",  "getBalance"),
        # Compliance checks
        ("ComplianceChecker.java",   "check"),
        # Ledger integrity
        ("LedgerService.java",       "record"),
        ("LedgerService.java",       "getEntriesForAccount"),
        ("LedgerService.java",       "computeBalance"),
        # Audit trail
        ("TransactionAudit.java",    "recordTransaction"),
        ("TransactionAudit.java",    "getRecordsForAccount"),
        ("TransactionAudit.java",    "getFailedTransactions"),
        ("TransactionAudit.java",    "verifyIntegrity"),
        # Transfer validation
        ("TransferValidator.java",   "validate"),
        ("TransferValidator.java",   "isWithinLimits"),
        # Account resolution
        ("AccountResolver.java",     "resolve"),
        ("AccountResolver.java",     "isActive"),
    ],
    "pii-service": [
        # PII masking
        ("pii.py", "mask_ssn"),
        ("pii.py", "mask_account"),
        ("pii.py", "is_valid_email"),
        ("pii.py", "redact_record"),
        # Input validation
        ("validators.py", "validate_phone"),
        ("validators.py", "validate_zip"),
        ("validators.py", "validate_date_of_birth"),
        ("validators.py", "sanitize_name"),
    ],
    "audit-service": [
        # Core audit trail
        ("audit.ts",            "logEvent"),
        ("audit.ts",            "getEvents"),
        ("audit.ts",            "getEventsByType"),
        ("audit.ts",            "clearEvents"),
        # Event formatting
        ("auditFormatter.ts",   "toJson"),
        ("auditFormatter.ts",   "toLogLine"),
        ("auditFormatter.ts",   "toCsv"),
        ("auditFormatter.ts",   "filterByTimeRange"),
        # Compliance reporting
        ("complianceReporter.ts","generateReport"),
        ("complianceReporter.ts","isBelowThreshold"),
        ("complianceReporter.ts","summarize"),
        # Event store
        ("eventStore.ts",       "persist"),
        ("eventStore.ts",       "findById"),
        ("eventStore.ts",       "findByUser"),
        # Integrity / retention
        ("integrityChecker.ts", "verifyChain"),
        ("integrityChecker.ts", "detectGaps"),
        ("retentionPolicy.ts",  "isExpired"),
        ("retentionPolicy.ts",  "shouldArchive"),
        ("retentionPolicy.ts",  "partitionEvents"),
    ],
}


# ---------------------------------------------------------------------------
# File helpers
# ---------------------------------------------------------------------------

def is_source_file(path: Path) -> bool:
    name = path.name
    if name.endswith(".d.ts"):
        return False
    return name.endswith(SOURCE_EXTENSIONS)


def is_test_file(rel_path: Path) -> bool:
    if any(part in ("test", "tests", "__tests__") for part in rel_path.parts):
        return True
    name = rel_path.name
    if name.endswith("Test.java"):
        return True
    if name.startswith("test_") and name.endswith(".py"):
        return True
    if name.endswith(".test.ts"):
        return True
    return False


def collect_test_content(service_dir: Path) -> str:
    """Return concatenated content of all test files in the service."""
    parts = []
    for root, _dirs, files in os.walk(service_dir):
        for fname in files:
            full = Path(root) / fname
            rel = full.relative_to(service_dir)
            if is_source_file(full) and is_test_file(rel):
                try:
                    parts.append(full.read_text(errors="replace"))
                except OSError:
                    pass
    return "\n".join(parts)


def count_files(service_dir: Path):
    sources = 0
    tests = 0
    for root, _dirs, files in os.walk(service_dir):
        for fname in files:
            full = Path(root) / fname
            rel = full.relative_to(service_dir)
            if not is_source_file(full):
                continue
            if is_test_file(rel):
                tests += 1
            else:
                sources += 1
    return sources, tests


# ---------------------------------------------------------------------------
# Compliance coverage
# ---------------------------------------------------------------------------

def compliance_coverage(service_name: str, service_dir: Path):
    """Return (covered, total, missing) for compliance-critical functions."""
    funcs = COMPLIANCE_FUNCTIONS.get(service_name, [])
    if not funcs:
        return 0, 0, []

    test_content = collect_test_content(service_dir)

    covered = []
    missing = []
    for src_file, fn_name in funcs:
        if fn_name in test_content:
            covered.append((src_file, fn_name))
        else:
            missing.append((src_file, fn_name))

    return len(covered), len(funcs), missing


def format_pct(numerator: int, denominator: int) -> str:
    if denominator == 0:
        return "N/A"
    pct = int(round(numerator / denominator * 100))
    if pct == 0:
        return "0%"
    if pct == 100:
        return "100%"
    return f"~{pct}%"


def format_file_pct(cov: float) -> str:
    pct = int(round(cov * 100))
    if pct == 0:
        return "0%"
    if pct == 100:
        return "100%"
    return f"~{pct}%"


def file_coverage_ratio(sources: int, tests: int) -> float:
    if sources == 0:
        return 0.0
    return min(tests / sources, 1.0)


# ---------------------------------------------------------------------------
# Report builders
# ---------------------------------------------------------------------------

def build_summary_table(rows) -> str:
    """Compact table printed to stdout (used by CI for PR comments)."""
    lines = [
        f"| {'Service':<22} | {'File Coverage':<13} | {'Compliance Fns':<14} |",
        f"|{'-' * 24}|{'-' * 15}|{'-' * 16}|",
    ]
    total_src = total_test = total_covered = total_funcs = 0
    for name, src, tst, cov_covered, cov_total, _missing in rows:
        file_pct = format_file_pct(file_coverage_ratio(src, tst))
        fn_pct   = format_pct(cov_covered, cov_total)
        lines.append(f"| {name:<22} | {file_pct:<13} | {fn_pct:<14} |")
        total_src     += src
        total_test    += tst
        total_covered += cov_covered
        total_funcs   += cov_total
    overall_file = format_file_pct(file_coverage_ratio(total_src, total_test))
    overall_fn   = format_pct(total_covered, total_funcs)
    lines.append(f"| {'**Overall**':<22} | {overall_file:<13} | {overall_fn:<14} |")
    return "\n".join(lines)


def build_audit_report(rows) -> str:
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d")

    total_src = total_test = total_covered = total_funcs = 0
    for _, src, tst, cov_covered, cov_total, _ in rows:
        total_src     += src
        total_test    += tst
        total_covered += cov_covered
        total_funcs   += cov_total

    overall_file = format_file_pct(file_coverage_ratio(total_src, total_test))
    overall_fn   = format_pct(total_covered, total_funcs)

    # Per-service file + compliance table
    summary_table = build_summary_table(rows)

    # Per-service compliance detail
    detail_sections = []
    for name, src, tst, cov_covered, cov_total, missing in rows:
        file_pct = format_file_pct(file_coverage_ratio(src, tst))
        fn_pct   = format_pct(cov_covered, cov_total)
        if missing:
            missing_lines = "\n".join(
                f"  - `{fn}` (in `{sf}`)" for sf, fn in missing
            )
            gap_block = f"**Uncovered compliance functions ({len(missing)}):**\n{missing_lines}"
        else:
            gap_block = "All compliance-critical functions are covered."
        detail_sections.append(
            f"### {name}\n\n"
            f"- File coverage: **{file_pct}** ({tst} test file(s) / {src} source file(s))\n"
            f"- Compliance function coverage: **{fn_pct}** ({cov_covered}/{cov_total} functions)\n\n"
            f"{gap_block}"
        )
    detail_block = "\n\n".join(detail_sections)

    # Assessment narrative
    assessment_lines = []
    for name, _s, _t, cov_covered, cov_total, missing in rows:
        fn_pct = format_pct(cov_covered, cov_total)
        if cov_total == 0:
            assessment_lines.append(f"- **{name}**: no compliance functions defined.")
        elif not missing:
            assessment_lines.append(f"- **{name}**: all {cov_total} compliance functions covered ({fn_pct}).")
        else:
            assessment_lines.append(
                f"- **{name}**: {cov_covered}/{cov_total} compliance functions covered ({fn_pct}). "
                f"{len(missing)} function(s) lack test coverage."
            )
    assessment = "\n".join(assessment_lines) if assessment_lines else "- No data."

    return f"""# Coverage Audit Report

**Report ID:** AUDIT-{now}  
**Generated:** {now}  
**Type:** Coverage Assessment  
**Scope:** All ClearBank Core services  

---

## Summary

| Metric | Value |
|---|---|
| Overall file coverage | **{overall_file}** |
| Overall compliance function coverage | **{overall_fn}** ({total_covered}/{total_funcs} functions) |

## Coverage by Service

{summary_table}

## Compliance Function Coverage Detail

{detail_block}

## Assessment

{assessment}

---

*This is an automated audit report generated by ClearBank's coverage tooling.*
"""


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main() -> None:
    rows = []
    for name in SERVICES:
        service_dir = Path(name)
        if not service_dir.exists():
            continue
        src, tst = count_files(service_dir)
        cov_covered, cov_total, missing = compliance_coverage(name, service_dir)
        rows.append((name, src, tst, cov_covered, cov_total, missing))

    # Print summary table to stdout (used by CI / PR comments)
    print(build_summary_table(rows))

    # Write full audit report
    AUDIT_REPORTS_DIR.mkdir(exist_ok=True)
    (AUDIT_REPORTS_DIR / "coverage.md").write_text(build_audit_report(rows))


if __name__ == "__main__":
    main()
