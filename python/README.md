# ciencia-id

Validation of the Ciência ID check digit (ISO/IEC 7064, MOD 17,16). Zero dependencies, Python 3.9+.

## Install

```sh
pip install ciencia-id
```

## Usage

```python
from ciencia_id import is_valid, parse, compute_check_digit, normalize, format, SOURCES

is_valid("EA11-91C2-39D1")                 # True
compute_check_digit("EA1191C239D")         # "1"  (11 hex digits in, check digit out; None if malformed)
normalize(" https://www.ciencia-id.pt/ea11-91c2-39d1\n")  # "EA1191C239D1"
format("ea1191c239d1")                     # "EA11-91C2-39D1"  (the check digit is not verified)
format("ea1191c239d1", True)               # "ciencia-id.pt/EA11-91C2-39D1"
parse("EA11-91C2-39D1")
# ParseResult(
#   status="CIENCIA_ID", reason=None, warnings=(),
#   internal_validation="EA",
#   source=Source(code="1", known=True, name="Ciência ID", domain="ciencia-id.pt"),
#   unique_id="191C239D", unique_id_decimal=421274525,
#   check_digit="1", indicated_domain=None)
```

Input is trimmed of ASCII whitespace, upper-cased (ASCII only), stripped of an optional
`http(s)://`, `www.` and `ciencia-id.pt/` or `estudante-id.pt/` prefix, and stripped of `-`.
Anything else (en dashes, no-break spaces, inner spaces, ligatures) is rejected as `FORMAT`.

## Statuses

| `status` | Meaning |
| --- | --- |
| `CIENCIA_ID` | Well-formed, check digit correct, known source. |
| `UNKNOWN_SOURCE` | Well-formed, check digit correct, but the source code (3rd digit) is not known to this library. It may be a typo that the check digit happened to miss, or the library may be out of date. |
| `INVALID` | See `reason`: `FORMAT` (not 12 hex digits after normalisation) or `CHECK_DIGIT` (check digit mismatch). All other fields are `None`. |

`CIENCIA_ID` does not guarantee that the ID was ever issued, nor does it verify the internal validation
digits; it only says the check digit is consistent.

## `DOMAIN_MISMATCH`

`warnings` contains `"DOMAIN_MISMATCH"` when the input carried a domain prefix that differs from the
domain of the ID's source (for example `estudante-id.pt/EA11-91C2-39D1`).

## Historical note

Ciência ID continues the Estudante-ID identifier. The `estudante-id.pt` prefix is still accepted on input,
but IDs from source `1` belong to `ciencia-id.pt`, so such input yields `DOMAIN_MISMATCH`.

## Known limitation

A single check digit cannot detect every error. Swapping the last two characters of an ID ending in `…BC01`
gives `…BC10`, which also validates. This is a property of the algorithm, not a bug.

## License

Apache-2.0. See `LICENSE` and `NOTICE`.
