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

Or copy `include/ciencia_id.hpp` (copy `LICENSE` and `NOTICE` with it).
Used as a subproject, the library builds no tests and downloads nothing.

## Updates

C++ has no package registry, and Dependabot does not read `FetchContent`.
[Renovate](https://docs.renovatebot.com/) can open a pull request that bumps
`GIT_TAG` whenever a `cpp-v*` tag is released. Add this to your `renovate.json`:

```json
{
  "customManagers": [
    {
      "customType": "regex",
      "managerFilePatterns": ["/(^|/)CMakeLists\\.txt$/"],
      "matchStrings": [
        "GIT_REPOSITORY\\s+https://github\\.com/peopleware-pt/ciencia-id_checkdigit(\\.git)?\\s+GIT_TAG\\s+cpp-v(?<currentValue>\\d+\\.\\d+\\.\\d+)"
      ],
      "depNameTemplate": "peopleware-pt/ciencia-id_checkdigit",
      "datasourceTemplate": "github-tags",
      "extractVersionTemplate": "^cpp-v(?<version>.+)$"
    }
  ]
}
```

The pattern expects `GIT_TAG` right after `GIT_REPOSITORY`, as in the example
above; tags of the other languages are ignored.

For notifications only, GitHub's *Watch → Custom → Releases* cannot filter by
tag and reports the releases of every language. C++ release titles start with
`C++ v`, so filter on that.

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
The warning does not change `status` or `isValid`. For an unknown source, any domain prefix counts as a mismatch.

## Historical note

The check-digit mechanism was designed for two identifiers: Ciência ID (in use, source `1`) and Estudante-ID, which was never implemented, has no source code assigned and will not get one. The library is named after Ciência ID, but the algorithm also supports Estudante-ID, for historical reasons: the `estudante-id.pt/` prefix is accepted on input and yields the `DOMAIN_MISMATCH` warning.

## Known limitation

The check digit detects every single-character substitution, but not every adjacent transposition: at each position exactly one of the 120 possible pairs of distinct hex digits can be swapped undetected (about 1 in 120). In particular, swapping the last data digit and the check digit goes undetected when they are `0` and `1`, so an ID ending in `01` and the same ID ending in `10` both validate (e.g. `1C11-C5F1-BC01` and `1C11-C5F1-BC10`).

## Tests

```sh
cmake -S . -B build && cmake --build build && ctest --test-dir build --output-on-failure
```

## License

Apache-2.0. See the repository `LICENSE` and `NOTICE`.
