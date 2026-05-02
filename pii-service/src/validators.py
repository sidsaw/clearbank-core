"""Validation rules for PII fields before storage or transmission."""

import re

PHONE_PATTERN = re.compile(r"^\+?1?\d{10,15}$")
ZIP_PATTERN = re.compile(r"^\d{5}(-\d{4})?$")


def validate_phone(phone: str) -> bool:
    if not isinstance(phone, str):
        return False
    cleaned = re.sub(r"[\s\-\(\)]", "", phone)
    return bool(PHONE_PATTERN.match(cleaned))


def validate_zip(zipcode: str) -> bool:
    if not isinstance(zipcode, str):
        return False
    return bool(ZIP_PATTERN.match(zipcode))


def validate_date_of_birth(dob: str) -> bool:
    if not isinstance(dob, str):
        return False
    parts = dob.split("-")
    if len(parts) != 3:
        return False
    try:
        year, month, day = int(parts[0]), int(parts[1]), int(parts[2])
    except ValueError:
        return False
    return 1900 <= year <= 2100 and 1 <= month <= 12 and 1 <= day <= 31


def sanitize_name(name: str) -> str:
    if not isinstance(name, str):
        raise ValueError("Name must be a string")
    return re.sub(r"[^a-zA-Z\s\-']", "", name).strip()
