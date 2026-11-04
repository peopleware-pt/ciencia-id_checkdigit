# ciencia-id (C++)

Validation of the Ciência ID check digit (ISO/IEC 7064, MOD 17,16). Header-only C++17, zero dependencies.

## Install

With CMake `FetchContent`:

```cmake
include(FetchContent)
FetchContent_Declare(cienciaid
  GIT_REPOSITORY https://github.com/peopleware-pt/ciencia-id_checkdigit.git
  GIT_TAG cpp-v1.0.0
  SOURCE_SUBDIR cpp)
FetchContent_MakeAvailable(cienciaid)

target_link_libraries(your_target PRIVATE cienciaid::cienciaid)
```

Or copy `include/ciencia_id.hpp`. Track updates through the GitHub Releases tagged `cpp-v*`.
Used as a subproject, the library builds no tests and downloads nothing.

## Usage

```cpp
#include <ciencia_id.hpp>

cienciaid::isValid("EA11-91C2-39D1");                 // true
cienciaid::computeCheckDigit("EA1191C239D");          // "1"  (11 hex digits in; std::nullopt if malformed)
cienciaid::normalize(" https://www.ciencia-id.pt/ea11-91c2-39d1\n"); // "EA1191C239D1"
cienciaid::format("ea1191c239d1");                    // "EA11-91C2-39D1"  (the check digit is not verified)
cienciaid::format("ea1191c239d1", true);              // "ciencia-id.pt/EA11-91C2-39D1"

auto r = cienciaid::parse("EA11-91C2-39D1");
// r.status == Status::CienciaId, r.reason == nullopt, r.warnings empty
// *r.internalValidation == "EA"
// r.source->code == "1", known, name "Ciência ID", domain "ciencia-id.pt"
// *r.uniqueId == "191C239D", *r.uniqueIdDecimal == 421274525 (std::uint32_t)
// *r.checkDigit == "1", r.indicatedDomain == nullopt
```

`to_string(Status | Reason | Warning)` returns the codes below. Input is trimmed of ASCII whitespace,
upper-cased (ASCII only), stripped of an optional `http(s)://`, `www.` and `ciencia-id.pt/` or
`estudante-id.pt/` prefix, and stripped of `-`. Anything else (en dashes, no-break spaces, inner spaces,
ligatures) is rejected as `FORMAT`. The header is pure ASCII; the name is stored as UTF-8 bytes.

## Statuses

| `status` | Meaning |
| --- | --- |
| `CIENCIA_ID` | Well-formed, check digit correct, known source. |
| `UNKNOWN_SOURCE` | Well-formed, check digit correct, but the source code (3rd digit) is not known to this library. It may be a typo that the check digit happened to miss, or the library may be out of date. |
| `INVALID` | See `reason`: `FORMAT` (not 12 hex digits after normalisation) or `CHECK_DIGIT` (check digit mismatch). All other fields are empty. |

`CIENCIA_ID` does not guarantee that the ID was ever issued, nor does it verify the internal validation
digits; it only says the check digit is consistent.

## `DOMAIN_MISMATCH`

`warnings` contains `Warning::DomainMismatch` when the input carried a domain prefix that differs from the
domain of the ID's source (for example `estudante-id.pt/EA11-91C2-39D1`).

## Historical note

Ciência ID continues the Estudante-ID identifier. The `estudante-id.pt` prefix is still accepted on input,
but IDs from source `1` belong to `ciencia-id.pt`, so such input yields `DOMAIN_MISMATCH`.

## Known limitation

A single check digit cannot detect every error. Swapping the last two characters of an ID ending in `…BC01`
gives `…BC10`, which also validates. This is a property of the algorithm, not a bug.

## Tests

```sh
cmake -S . -B build && cmake --build build && ctest --test-dir build --output-on-failure
```

## License

Apache-2.0. See the repository `LICENSE` and `NOTICE`.
