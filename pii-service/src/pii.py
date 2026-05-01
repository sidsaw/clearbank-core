"""Helpers for masking and validating PII fields in customer records."""

import re

SSN_PATTERN = re.compile(r"^\d{3}-\d{2}-\d{4}$")


def mask_ssn(ssn: str) -> str:
    if not isinstance(ssn, str) or not SSN_PATTERN.match(ssn):
        raise ValueError(f"Invalid SSN format: {ssn!r}")
    return "XXX-XX-" + ssn[-4:]


def mask_account(account_number: str) -> str:
    if not isinstance(account_number, str) or len(account_number) < 4:
        raise ValueError("Account number must be at least 4 characters")
    return "****" + account_number[-4:]


def is_valid_email(email: str) -> bool:
    if not isinstance(email, str):
        return False
    return "@" in email and "." in email.split("@")[-1]


def redact_record(record: dict) -> dict:
    redacted = dict(record)
    if "ssn" in redacted:
        redacted["ssn"] = mask_ssn(redacted["ssn"])
    if "account_number" in redacted:
        redacted["account_number"] = mask_account(redacted["account_number"])
    if "email" in redacted and not is_valid_email(redacted["email"]):
        redacted["email"] = None
    return redacted
