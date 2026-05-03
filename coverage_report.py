#!/usr/bin/env python3
"""Generate a coverage audit report across services in the monorepo."""

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

# Compliance-critical source files per service.
# These are the files whose functions directly implement regulatory requirements
# (authentication gates, ledger mutations, PII handling, audit trail integrity).
COMPLIANCE_CRITICAL_PATHS = {
    "auth-service": [
        "src/AuthService.java",
        "src/SessionManager.java",
        "src/TokenGenerator.java",
    ],
    "transaction-service": [
        "src/TransactionService.java",
        "src/ComplianceChecker.java",
        "src/TransactionAudit.java",
    ],
    "pii-service": [
        "src/pii.py",
        "src/validators.py",
    ],
    "audit-service": [
        "src/audit.ts",
        "src/complianceReporter.ts",
        "src/integrityChecker.ts",
    ],
}

# Mapping from compliance-critical source files to their expected test files.
COMPLIANCE_TEST_MAP = {
    "auth-service": {
        "src/AuthService.java": "src/test/AuthServiceTest.java",
        "src/SessionManager.java": "src/test/SessionManagerTest.java",
        "src/TokenGenerator.java": "src/test/TokenGeneratorTest.java",
    },
    "transaction-service": {
        "src/TransactionService.java": "src/test/TransactionServiceTest.java",
        "src/ComplianceChecker.java": "src/test/ComplianceCheckerTest.java",
        "src/TransactionAudit.java": "src/test/TransactionAuditTest.java",
    },
    "pii-service": {
        "src/pii.py": "tests/test_pii.py",
        "src/validators.py": "tests/test_validators.py",
    },
    "audit-service": {
        "src/audit.ts": "src/test/audit.test.ts",
        "src/complianceReporter.ts": "src/test/complianceReporter.test.ts",
        "src/integrityChecker.ts": "src/test/integrityChecker.test.ts",
    },
}


def is_source_file(path: Path) -> bool:
    name = path.name
    if name.endswith(".d.ts"):
        return False
    return name.endswith(SOURCE_EXTENSIONS)


def is_test_file(rel_path: Path) -> bool:
    if any(part in ("test", "tests") for part in rel_path.parts):
        return True
    name = rel_path.name
    if name.endswith("Test.java"):
        return True
    if name.startswith("test_") and name.endswith(".py"):
        return True
    if name.endswith(".test.ts"):
        return True
    return False


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


def coverage_estimate(sources: int, tests: int) -> float:
    if sources == 0:
        return 0.0
    return min(tests / sources, 1.0)


def format_pct(cov: float) -> str:
    pct = int(round(cov * 100))
    if pct == 0:
        return "0%"
    return f"~{pct}%"


def compute_compliance_coverage(service_name: str) -> list:
    """Compute coverage status for each compliance-critical path in a service.

    Returns a list of (source_file, has_test) tuples.
    """
    service_dir = Path(service_name)
    test_map = COMPLIANCE_TEST_MAP.get(service_name, {})
    results = []
    for src_file in COMPLIANCE_CRITICAL_PATHS.get(service_name, []):
        test_file = test_map.get(src_file, "")
        has_test = (service_dir / test_file).exists() if test_file else False
        results.append((src_file, has_test))
    return results


def build_table(rows, overall_label: str) -> str:
    overall_cell = f"**{overall_label}**" if overall_label != "0%" else "**0%**"
    lines = [
        f"| {'Service':<20} | {'Coverage Estimate':<17} |",
        f"|{'-' * 22}|{'-' * 19}|",
    ]
    for name, _s, _t, cov in rows:
        lines.append(f"| {name:<20} | {format_pct(cov):<17} |")
    lines.append(f"| {'**Overall**':<20} | {overall_cell:<17} |")
    return "\n".join(lines)


def build_compliance_section(compliance_data: dict) -> str:
    """Build the Compliance Critical Paths Coverage section."""
    lines = []
    total_paths = 0
    total_covered = 0

    for service_name in SERVICES:
        results = compliance_data.get(service_name, [])
        if not results:
            continue
        covered = sum(1 for _, has_test in results if has_test)
        total = len(results)
        total_paths += total
        total_covered += covered
        pct = int(round((covered / total) * 100)) if total > 0 else 0

        lines.append(f"### {service_name} — {covered}/{total} paths covered ({pct}%)")
        lines.append("")
        lines.append("| Critical Path | Test Coverage |")
        lines.append("|---|---|")
        for src_file, has_test in results:
            status = "Covered" if has_test else "**Not covered**"
            lines.append(f"| `{src_file}` | {status} |")
        lines.append("")

    overall_pct = (
        int(round((total_covered / total_paths) * 100)) if total_paths > 0 else 0
    )
    summary = (
        f"**Overall compliance path coverage: {total_covered}/{total_paths} "
        f"({overall_pct}%)**"
    )

    return summary + "\n\n" + "\n".join(lines)


def build_audit_report(
    table: str, overall_label: str, rows, compliance_data: dict
) -> str:
    now = datetime.now(timezone.utc).strftime("%Y-%m-%d")
    zero_coverage = [name for name, _s, _t, cov in rows if cov == 0.0]
    has_coverage = [
        (name, format_pct(cov)) for name, _s, _t, cov in rows if cov > 0.0
    ]

    assessment_lines = []
    for name, pct in has_coverage:
        assessment_lines.append(f"- **{name}** has {pct} estimated coverage.")
    if zero_coverage:
        names = ", ".join(f"**{n}**" for n in zero_coverage)
        assessment_lines.append(f"- {names} — zero test coverage.")

    assessment = "\n".join(assessment_lines) if assessment_lines else "- No data."

    compliance_section = build_compliance_section(compliance_data)

    return f"""# Coverage Audit Report

**Report ID:** AUDIT-{now}  
**Generated:** {now}  
**Type:** Coverage Assessment  
**Scope:** All ClearBank Core services  

---

## Summary

Overall estimated test coverage: **{overall_label}**.

## Coverage by Service

{table}

## Compliance Critical Paths Coverage

{compliance_section}

## Assessment

{assessment}

---

*This is an automated audit report generated by ClearBank's coverage tooling.*
"""


def main() -> None:
    rows = []
    total_src = 0
    total_test = 0
    for name in SERVICES:
        service_dir = Path(name)
        if not service_dir.exists():
            continue
        s, t = count_files(service_dir)
        total_src += s
        total_test += t
        rows.append((name, s, t, coverage_estimate(s, t)))

    overall = coverage_estimate(total_src, total_test)
    overall_label = format_pct(overall)

    table = build_table(rows, overall_label)

    # Compute compliance critical paths coverage
    compliance_data = {}
    for name in SERVICES:
        if Path(name).exists():
            compliance_data[name] = compute_compliance_coverage(name)

    # Print the simple table to stdout (used by CI for PR comments)
    print(table)
    print()
    print("## Compliance Critical Paths Coverage")
    print()
    print(build_compliance_section(compliance_data))

    # Write full audit report to audit_reports/
    AUDIT_REPORTS_DIR.mkdir(exist_ok=True)
    audit_report = build_audit_report(table, overall_label, rows, compliance_data)
    (AUDIT_REPORTS_DIR / "coverage.md").write_text(audit_report)


if __name__ == "__main__":
    main()
