# ciencia-id (Java)

Validation of the Ciência ID check digit (ISO/IEC 7064, MOD 17,16). Zero runtime dependencies, Java 8+.

## Install

Maven:

```xml
<dependency>
  <groupId>pt.peopleware</groupId>
  <artifactId>ciencia-id</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle:

```groovy
implementation 'pt.peopleware:ciencia-id:1.0.0'
```

## Usage

```java
import pt.peopleware.cienciaid.*;

CienciaId.isValid("EA11-91C2-39D1");                 // true
CienciaId.computeCheckDigit("EA1191C239D");          // Optional["1"] (11 hex digits in; empty if malformed)
CienciaId.normalize(" https://www.ciencia-id.pt/ea11-91c2-39d1\n"); // "EA1191C239D1"
CienciaId.format("ea1191c239d1");                    // Optional["EA11-91C2-39D1"] (check digit not verified)
CienciaId.format("ea1191c239d1", true);              // Optional["ciencia-id.pt/EA11-91C2-39D1"]

ParseResult r = CienciaId.parse("EA11-91C2-39D1");
r.getStatus();             // Status.CIENCIA_ID
r.getReason();             // null
r.getWarnings();           // [] (unmodifiable List<Warning>)
r.getInternalValidation(); // "EA"
r.getSource();             // code "1", known, name "Ciência ID", domain "ciencia-id.pt"
r.getUniqueId();           // "191C239D"
r.getUniqueIdDecimal();    // 421274525L (Long; unsigned 32-bit range)
r.getCheckDigit();         // "1"
r.getIndicatedDomain();    // null
```

All methods accept `null`. Input is trimmed of ASCII whitespace, upper-cased (ASCII only, independent of the
default locale), stripped of an optional `http(s)://`, `www.` and `ciencia-id.pt/` or `estudante-id.pt/`
prefix, and stripped of `-`. Anything else (en dashes, no-break spaces, inner spaces, ligatures) is rejected
as `FORMAT`.

## Statuses

| `Status` | Meaning |
| --- | --- |
| `CIENCIA_ID` | Well-formed, check digit correct, known source. |
| `UNKNOWN_SOURCE` | Well-formed, check digit correct, but the source code (3rd digit) is not known to this library. It may be a typo that the check digit happened to miss, or the library may be out of date. |
| `INVALID` | See `getReason()`: `FORMAT` (not 12 hex digits after normalisation) or `CHECK_DIGIT` (check digit mismatch). All other fields are `null`. |

`CIENCIA_ID` does not guarantee that the ID was ever issued, nor does it verify the internal validation
digits; it only says the check digit is consistent.

## `DOMAIN_MISMATCH`

`getWarnings()` contains `Warning.DOMAIN_MISMATCH` when the input carried a domain prefix that differs from
the domain of the ID's source (for example `estudante-id.pt/EA11-91C2-39D1`).
The warning does not change `status` or `isValid()`. For an unknown source, any domain prefix counts as a mismatch.

## Historical note

The check-digit mechanism was designed for two identifiers: Ciência ID (in use, source `1`) and Estudante-ID, which was never implemented, has no source code assigned and will not get one. The library is named after Ciência ID, but the algorithm also supports Estudante-ID, for historical reasons: the `estudante-id.pt/` prefix is accepted on input and yields the `DOMAIN_MISMATCH` warning.

## Known limitation

The check digit detects every single-character substitution, but not every adjacent transposition: at each position exactly one of the 120 possible pairs of distinct hex digits can be swapped undetected (about 1 in 120). In particular, swapping the last data digit and the check digit goes undetected when they are `0` and `1`, so an ID ending in `01` and the same ID ending in `10` both validate (e.g. `1C11-C5F1-BC01` and `1C11-C5F1-BC10`).

## License

Apache-2.0. `LICENSE` and `NOTICE` are packaged in the jar under `META-INF/`.
