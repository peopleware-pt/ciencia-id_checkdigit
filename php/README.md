# peopleware/ciencia-id

Validation of the Ciência ID check digit (ISO/IEC 7064, MOD 17,16). Zero dependencies, PHP 7.4+.

> The `ciencia-id-php` repository is a read-only mirror. Issues and pull requests go to
> [`peopleware-pt/ciencia-id_checkdigit`](https://github.com/peopleware-pt/ciencia-id_checkdigit).

## Install

```sh
composer require peopleware/ciencia-id
```

## Usage

```php
use Peopleware\CienciaId\CienciaId;

CienciaId::isValid('EA11-91C2-39D1');                 // true
CienciaId::computeCheckDigit('EA1191C239D');          // "1"  (11 hex digits in, check digit out; null if malformed)
CienciaId::normalize(" https://www.ciencia-id.pt/ea11-91c2-39d1\n"); // "EA1191C239D1"
CienciaId::format('ea1191c239d1');                    // "EA11-91C2-39D1"  (the check digit is not verified)
CienciaId::format('ea1191c239d1', true);              // "ciencia-id.pt/EA11-91C2-39D1"
CienciaId::parse('EA11-91C2-39D1');
// [
//   'status' => 'CIENCIA_ID', 'reason' => null, 'warnings' => [],
//   'internalValidation' => 'EA',
//   'source' => ['code' => '1', 'known' => true, 'name' => 'Ciência ID', 'domain' => 'ciencia-id.pt'],
//   'uniqueId' => '191C239D', 'uniqueIdDecimal' => 421274525,
//   'checkDigit' => '1', 'indicatedDomain' => null,
// ]
```

Input is trimmed of ASCII whitespace, upper-cased (ASCII only), stripped of an optional
`http(s)://`, `www.` and `ciencia-id.pt/` or `estudante-id.pt/` prefix, and stripped of `-`.
Anything else (en dashes, no-break spaces, inner spaces, ligatures) is rejected as `FORMAT`.

## Statuses

| `status` | Meaning |
| --- | --- |
| `CIENCIA_ID` | Well-formed, check digit correct, known source. |
| `UNKNOWN_SOURCE` | Well-formed, check digit correct, but the source code (3rd digit) is not known to this library. It may be a typo that the check digit happened to miss, or the library may be out of date. |
| `INVALID` | See `reason`: `FORMAT` (not 12 hex digits after normalisation) or `CHECK_DIGIT` (check digit mismatch). All other fields are `null`. |

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

## Tests

```sh
php tests/run.php
```

## License

Apache-2.0. See `LICENSE` and `NOTICE`.
