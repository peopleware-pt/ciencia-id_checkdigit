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

Non-string input (`bytes`, numbers, lists, ...) is treated as invalid (`FORMAT`), like `None`.

Prefer `import ciencia_id` and call `ciencia_id.format(...)`: `from ciencia_id import format` shadows the builtin `format`.

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
The warning does not change `status` or `is_valid`. For an unknown source, any domain prefix counts as a mismatch.

## Historical note

The check-digit mechanism was designed for two identifiers: Ciência ID (in use, source `1`) and Estudante-ID, which was never implemented, has no source code assigned and will not get one. The library is named after Ciência ID, but the algorithm also supports Estudante-ID, for historical reasons: the `estudante-id.pt/` prefix is accepted on input and yields the `DOMAIN_MISMATCH` warning.

## Known limitation

The check digit detects every single-character substitution, but not every adjacent transposition: at each position exactly one of the 120 possible pairs of distinct hex digits can be swapped undetected (about 1 in 120). In particular, swapping the last data digit and the check digit goes undetected when they are `0` and `1`, so an ID ending in `01` and the same ID ending in `10` both validate (e.g. `1C11-C5F1-BC01` and `1C11-C5F1-BC10`).

## License

Apache-2.0. See `LICENSE` and `NOTICE`.
