# Ciência ID check digit

Zero-dependency libraries that validate the check digit of a **Ciência ID**
(ISO/IEC 7064, MOD 17,16), with the same API and the same behaviour in
JavaScript, Python, Java, PHP and C++. All five read one shared contract,
[`test-vectors.json`](test-vectors.json).

## Historical note

The check-digit mechanism was designed for two identifiers: Ciência ID (in use,
source `1`) and Estudante-ID, which was never implemented, has no source code
assigned and will not get one. The library is named after Ciência ID, but the
algorithm also supports Estudante-ID, for historical reasons: the
`estudante-id.pt/` prefix is accepted on input and yields the `DOMAIN_MISMATCH`
warning.

The algorithm is described in «Estudante-ID – Regras de uso do Identificador
para Parceiros» v1.0 (MCTES, 2016).

## Identifier structure

| Position | Field (API)          | Length | Notes |
|----------|----------------------|--------|-------|
| 1–2      | `internalValidation` | 2 hex  | Internal hash with a salt; only the issuer can verify it |
| 3        | `source`             | 1 hex  | Registering entity; only `1` (Ciência ID) is known |
| 4–11     | `uniqueId`           | 8 hex  | Unsigned 32-bit integer, not sequential |
| 12       | `checkDigit`         | 1 hex  | ISO/IEC 7064, MOD 17,16 |

Forms: machine-to-machine `EA1191C239D1`; human-readable `EA11-91C2-39D1`;
URI `ciencia-id.pt/EA11-91C2-39D1`. Input is trimmed (ASCII whitespace),
upper-cased (ASCII), an optional `http(s)://`, `www.`, `ciencia-id.pt/` or
`estudante-id.pt/` prefix is removed, and hyphens are dropped. What remains
must be exactly 12 hex digits.

## Statuses

| `status`         | Meaning |
|------------------|---------|
| `INVALID`        | Not 12 hex digits after normalisation (`reason: FORMAT`), or wrong check digit (`reason: CHECK_DIGIT`) |
| `CIENCIA_ID`     | Source `1` and correct check digit. This does **not** guarantee the ID was assigned, nor that it is well formed: the internal validation (positions 1–2) is known only to the issuer |
| `UNKNOWN_SOURCE` | Correct check digit but a source other than `1`: a potential error — the source is unknown, **or** this library is out of date |

`isValid` is true only for `CIENCIA_ID`. The `DOMAIN_MISMATCH` warning is raised
when the domain given in the input is not the source's domain; it does not change
`status` or `isValid`. For an unknown source, any domain prefix counts as a mismatch.

## Languages

Each implementation is versioned and released on its own, with tags
`<lang>-vX.Y.Z`.

| Language   | Package | Install | Docs |
|------------|---------|---------|------|
| JavaScript | `@peopleware-pt/ciencia-id` (npm) | `npm install @peopleware-pt/ciencia-id` | [js/](js/README.md) |
| Python     | `ciencia-id` (PyPI) | `pip install ciencia-id` | [python/](python/README.md) |
| Java       | `pt.peopleware:ciencia-id` (Maven Central) | Maven/Gradle dependency | [java/](java/README.md) |
| PHP        | `peopleware/ciencia-id` (Packagist) | `composer require peopleware/ciencia-id` | [php/](php/README.md) |
| C++17      | header-only | CMake `FetchContent` with `SOURCE_SUBDIR cpp` | [cpp/](cpp/README.md) |

## Receiving updates

Depend on the package for your language through its registry and let
Dependabot or Renovate propose new versions; a change in one language does not
release the others. For C++, watch the GitHub Releases tagged `cpp-v*` and bump
the `GIT_TAG` in your `FetchContent_Declare`.

## Known limitation

The check digit detects every single-character substitution, but not every
adjacent transposition: at each position exactly one of the 120 possible pairs of
distinct hex digits can be swapped undetected (about 1 in 120). In particular,
swapping the last data digit and the check digit goes undetected when they are
`0` and `1`, so an ID ending in `01` and the same ID ending in `10` both validate
(e.g. `1C11-C5F1-BC01` and `1C11-C5F1-BC10`).

## License

[Apache-2.0](LICENSE). See [NOTICE](NOTICE).
Copyright 2026 Paulo Soares (PeopleWare®).
