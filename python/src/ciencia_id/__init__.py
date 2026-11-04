"""Ciência ID check digit (ISO/IEC 7064, MOD 17,16).

Implements the pseudo-code of «Estudante-ID – Regras de uso do Identificador para Parceiros» v1.0.
"""
import re
import string
from dataclasses import dataclass
from types import MappingProxyType
from typing import Mapping, Optional, Tuple

__all__ = ["SOURCES", "Source", "ParseResult", "compute_check_digit", "is_valid", "parse", "normalize", "format"]


@dataclass(frozen=True)
class Source:
    code: str
    known: bool
    name: str
    domain: Optional[str]


@dataclass(frozen=True)
class ParseResult:
    status: str
    reason: Optional[str]
    warnings: Tuple[str, ...]
    internal_validation: Optional[str]
    source: Optional[Source]
    unique_id: Optional[str]
    unique_id_decimal: Optional[int]
    check_digit: Optional[str]
    indicated_domain: Optional[str]


SOURCES = MappingProxyType({
    "1": Source("1", True, "Ciência ID", "ciencia-id.pt"),
})  # type: Mapping[str, Source]

_DOMAIN_RE = re.compile(r"(?:HTTPS?://)?(?:WWW\.)?((?:CIENCIA|ESTUDANTE)-ID\.PT)/")
_ASCII_UPPER = str.maketrans(string.ascii_lowercase, string.ascii_uppercase)
_ASCII_WS = " \t\n\r\f\v"


def _f(x):
    v = x % 16
    return 16 if v == 0 else v


def _split(value):
    """Return (s, indicated_domain): canonical upper-case ID text and the domain prefix found, if any.

    Only ASCII whitespace is trimmed and only ASCII letters are upper-cased (no Unicode folding).
    """
    if value is None:
        return "", None
    s = str(value).strip(_ASCII_WS).translate(_ASCII_UPPER)
    indicated = None
    m = _DOMAIN_RE.match(s)
    if m:
        indicated = m.group(1).lower()
        s = s[m.end():]
    return s.replace("-", ""), indicated


def normalize(value: Optional[str]) -> str:
    return _split(value)[0]


def _check_digit_of(s):
    t = 16
    for c in s[:11]:
        t = (2 * _f(t + int(c, 16))) % 17
    return "0123456789ABCDEF"[(17 - t) % 16]


def compute_check_digit(value: Optional[str]) -> Optional[str]:
    s = normalize(value)
    return _check_digit_of(s) if re.fullmatch(r"[0-9A-F]{11}", s) else None


def _source_of(code):
    return SOURCES.get(code) or Source(code, False, "Unknown", None)


def _invalid(reason):
    return ParseResult("INVALID", reason, (), None, None, None, None, None, None)


def parse(value: Optional[str]) -> ParseResult:
    s, indicated = _split(value)
    if not re.fullmatch(r"[0-9A-F]{12}", s):
        return _invalid("FORMAT")
    if _check_digit_of(s) != s[11]:
        return _invalid("CHECK_DIGIT")
    source = _source_of(s[2])
    unique_id = s[3:11]
    mismatch = indicated is not None and indicated != source.domain
    return ParseResult(
        "CIENCIA_ID" if source.known else "UNKNOWN_SOURCE",
        None,
        ("DOMAIN_MISMATCH",) if mismatch else (),
        s[:2],
        source,
        unique_id,
        int(unique_id, 16),
        s[11],
        indicated,
    )


def is_valid(value: Optional[str]) -> bool:
    return parse(value).status == "CIENCIA_ID"


def format(value: Optional[str], with_domain: bool = False) -> Optional[str]:  # noqa: A001
    """Format 12 hex digits as XXXX-XXXX-XXXX (the check digit is NOT verified)."""
    s = normalize(value)
    if not re.fullmatch(r"[0-9A-F]{12}", s):
        return None
    text = s[:4] + "-" + s[4:8] + "-" + s[8:]
    source = _source_of(s[2])
    return source.domain + "/" + text if with_domain and source.known else text
