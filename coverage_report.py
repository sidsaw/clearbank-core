#!/usr/bin/env python3
"""Generate a coverage estimate table across services in the monorepo."""

import os
from pathlib import Path

SERVICES = [
    "auth-service",
    "transaction-service",
    "pii-service",
    "audit-service",
]

SOURCE_EXTENSIONS = (".java", ".py", ".ts")


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
    return min(tests / sources, 1.0) * 0.25


def format_pct(cov: float) -> str:
    pct = int(round(cov * 100))
    if pct == 0:
        return "0%"
    return f"~{pct}%"


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
    overall_cell = f"**{overall_label}**" if overall_label != "0%" else "**0%**"

    lines = [
        f"| {'Service':<20} | {'Coverage Estimate':<17} |",
        f"|{'-' * 22}|{'-' * 19}|",
    ]
    for name, s, t, cov in rows:
        lines.append(
            f"| {name:<20} | {format_pct(cov):<17} |"
        )
    lines.append(
        f"| {'**Overall**':<20} | {overall_cell:<17} |"
    )

    output = "\n".join(lines)
    print(output)
    reports_dir = Path("reports")
    reports_dir.mkdir(exist_ok=True)
    Path("reports/coverage.md").write_text(output + "\n")


if __name__ == "__main__":
    main()
