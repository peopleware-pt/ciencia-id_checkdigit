// Ciencia ID check digit (ISO/IEC 7064, MOD 17,16). Header-only, C++17.
// Implements the pseudo-code of "Estudante-ID - Regras de uso do Identificador para Parceiros" v1.0.
#ifndef CIENCIA_ID_HPP
#define CIENCIA_ID_HPP

#include <cstddef>
#include <cstdint>
#include <map>
#include <optional>
#include <string>
#include <string_view>
#include <utility>
#include <vector>

namespace cienciaid {

enum class Status { Invalid, CienciaId, UnknownSource };
enum class Reason { Format, CheckDigit };
enum class Warning { DomainMismatch };

inline const char* to_string(Status s) {
  switch (s) {
    case Status::Invalid: return "INVALID";
    case Status::CienciaId: return "CIENCIA_ID";
    case Status::UnknownSource: return "UNKNOWN_SOURCE";
  }
  return "";
}
inline const char* to_string(Reason r) {
  return r == Reason::Format ? "FORMAT" : "CHECK_DIGIT";
}
inline const char* to_string(Warning) { return "DOMAIN_MISMATCH"; }

struct Source {
  std::string code;
  bool known;
  std::string name;
  std::optional<std::string> domain;
};

inline const std::map<std::string, Source> SOURCES = {
    {"1", Source{"1", true, "Ci\xC3\xAAncia ID", std::string("ciencia-id.pt")}},
};

struct ParseResult {
  Status status = Status::Invalid;
  std::optional<Reason> reason;
  std::vector<Warning> warnings;
  std::optional<std::string> internalValidation;
  std::optional<Source> source;
  std::optional<std::string> uniqueId;
  std::optional<std::uint32_t> uniqueIdDecimal;
  std::optional<std::string> checkDigit;
  std::optional<std::string> indicatedDomain;
};

namespace detail {

// Explicit ASCII tests only: <cctype> is locale-dependent and undefined for negative char values.
inline bool isAsciiSpace(char c) {
  return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f' || c == '\v';
}

inline int hexValue(char c) {
  if (c >= '0' && c <= '9') return c - '0';
  if (c >= 'A' && c <= 'F') return c - 'A' + 10;
  return -1;
}

inline bool allHex(const std::string& s, std::size_t n) {
  if (s.size() != n) return false;
  for (char c : s)
    if (hexValue(c) < 0) return false;
  return true;
}

inline bool startsWith(const std::string& s, std::size_t pos, const char* p) {
  std::size_t i = 0;
  for (; p[i] != '\0'; ++i)
    if (pos + i >= s.size() || s[pos + i] != p[i]) return false;
  return true;
}

inline std::size_t plen(const char* p) {
  std::size_t n = 0;
  while (p[n] != '\0') ++n;
  return n;
}

struct Split {
  std::string s;
  std::optional<std::string> indicatedDomain;
};

// Trims ASCII whitespace, upper-cases ASCII letters only, strips an optional
// (HTTP://|HTTPS://)?(WWW.)?(CIENCIA-ID.PT|ESTUDANTE-ID.PT)/ prefix, removes '-'.
inline Split split(std::string_view input) {
  std::size_t b = 0, e = input.size();
  while (b < e && isAsciiSpace(input[b])) ++b;
  while (e > b && isAsciiSpace(input[e - 1])) --e;
  std::string s(input.substr(b, e - b));
  for (char& c : s)
    if (c >= 'a' && c <= 'z') c = static_cast<char>(c - 'a' + 'A');

  std::optional<std::string> domain;
  std::size_t pos = 0;
  if (startsWith(s, 0, "HTTPS://")) pos = 8;
  else if (startsWith(s, 0, "HTTP://")) pos = 7;
  if (startsWith(s, pos, "WWW.")) pos += 4;
  static const char* const names[] = {"CIENCIA-ID.PT", "ESTUDANTE-ID.PT"};
  for (const char* n : names) {
    const std::size_t len = plen(n);
    if (startsWith(s, pos, n) && pos + len < s.size() && s[pos + len] == '/') {
      std::string d(n);
      for (char& c : d) c = static_cast<char>(c >= 'A' && c <= 'Z' ? c - 'A' + 'a' : c);
      domain = d;
      s.erase(0, pos + len + 1);
      break;
    }
  }

  std::string out;
  for (char c : s)
    if (c != '-') out.push_back(c);
  return Split{out, domain};
}

inline int f(int x) {
  const int v = x % 16;
  return v == 0 ? 16 : v;
}

inline char checkDigitOf(const std::string& s) {
  int t = 16;
  for (std::size_t i = 0; i < 11; ++i) t = (2 * f(t + hexValue(s[i]))) % 17;
  return "0123456789ABCDEF"[(17 - t) % 16];
}

inline Source sourceOf(char code) {
  const std::string c(1, code);
  const auto it = SOURCES.find(c);
  if (it != SOURCES.end()) return it->second;
  return Source{c, false, "Unknown", std::nullopt};
}

}  // namespace detail

inline std::string normalize(std::string_view input) { return detail::split(input).s; }

inline std::optional<std::string> computeCheckDigit(std::string_view input) {
  const std::string s = normalize(input);
  if (!detail::allHex(s, 11)) return std::nullopt;
  return std::string(1, detail::checkDigitOf(s));
}

inline ParseResult parse(std::string_view input) {
  ParseResult r;
  const detail::Split sp = detail::split(input);
  const std::string& s = sp.s;
  if (!detail::allHex(s, 12)) {
    r.reason = Reason::Format;
    return r;
  }
  if (detail::checkDigitOf(s) != s[11]) {
    r.reason = Reason::CheckDigit;
    return r;
  }
  Source src = detail::sourceOf(s[2]);
  r.status = src.known ? Status::CienciaId : Status::UnknownSource;
  if (sp.indicatedDomain && sp.indicatedDomain != src.domain) r.warnings.push_back(Warning::DomainMismatch);
  r.internalValidation = s.substr(0, 2);
  r.uniqueId = s.substr(3, 8);
  std::uint32_t v = 0;
  for (char c : *r.uniqueId) v = (v << 4) | static_cast<std::uint32_t>(detail::hexValue(c));
  r.uniqueIdDecimal = v;
  r.checkDigit = std::string(1, s[11]);
  r.indicatedDomain = sp.indicatedDomain;
  r.source = std::move(src);
  return r;
}

inline bool isValid(std::string_view input) { return parse(input).status == Status::CienciaId; }

// Formats 12 hex digits as XXXX-XXXX-XXXX (the check digit is NOT verified).
inline std::optional<std::string> format(std::string_view input, bool withDomain = false) {
  const std::string s = normalize(input);
  if (!detail::allHex(s, 12)) return std::nullopt;
  const std::string text = s.substr(0, 4) + "-" + s.substr(4, 4) + "-" + s.substr(8);
  const Source src = detail::sourceOf(s[2]);
  if (withDomain && src.known) return *src.domain + "/" + text;
  return text;
}

}  // namespace cienciaid

#endif  // CIENCIA_ID_HPP
