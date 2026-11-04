#include <ciencia_id.hpp>

#include <nlohmann/json.hpp>

#include <cstdint>
#include <fstream>
#include <iostream>
#include <optional>
#include <string>
#include <utility>
#include <vector>

using nlohmann::json;
namespace ci = cienciaid;

static int failures = 0;

static void fail(const std::string& what, const std::string& input) {
  ++failures;
  std::cerr << "FAIL " << what << ": " << json(input).dump() << "\n";
}

template <class T>
static void check(const T& got, const T& want, const std::string& what, const std::string& input) {
  if (!(got == want)) fail(what, input);
}

static std::optional<std::string> opt_str(const json& j) {
  if (j.is_null()) return std::nullopt;
  return j.get<std::string>();
}

static std::string status_str(const json& j) { return j.get<std::string>(); }

static void check_parse(const std::string& input, const json& e) {
  const ci::ParseResult r = ci::parse(input);
  check(std::string(ci::to_string(r.status)), status_str(e["status"]), "status", input);
  check(r.reason ? std::optional<std::string>(ci::to_string(*r.reason)) : std::nullopt,
        opt_str(e["reason"]), "reason", input);
  std::vector<std::string> w;
  for (auto x : r.warnings) w.emplace_back(ci::to_string(x));
  check(w, e["warnings"].get<std::vector<std::string>>(), "warnings", input);
  check(r.internalValidation, opt_str(e["internalValidation"]), "internalValidation", input);
  check(r.uniqueId, opt_str(e["uniqueId"]), "uniqueId", input);
  check(r.uniqueIdDecimal,
        e["uniqueIdDecimal"].is_null() ? std::optional<std::uint32_t>()
                                       : std::optional<std::uint32_t>(e["uniqueIdDecimal"].get<std::uint32_t>()),
        "uniqueIdDecimal", input);
  check(r.checkDigit, opt_str(e["checkDigit"]), "checkDigit", input);
  check(r.indicatedDomain, opt_str(e["indicatedDomain"]), "indicatedDomain", input);
  const json& es = e["source"];
  if (es.is_null()) {
    if (r.source) fail("source not null", input);
  } else if (!r.source) {
    fail("source null", input);
  } else {
    check(r.source->code, es["code"].get<std::string>(), "source.code", input);
    check(r.source->known, es["known"].get<bool>(), "source.known", input);
    check(r.source->name, es["name"].get<std::string>(), "source.name", input);
    check(r.source->domain, opt_str(es["domain"]), "source.domain", input);
  }
  check(ci::isValid(input), e["status"] == "CIENCIA_ID", "isValid", input);
}

int main() {
  std::ifstream in(CIENCIAID_VECTORS, std::ios::binary);
  if (!in) { std::cerr << "cannot open " << CIENCIAID_VECTORS << "\n"; return 1; }
  const json V = json::parse(in);

  // 1. sources
  {
    const auto& s = ci::SOURCES.at("1");
    const json& e = V["sources"]["1"];
    check(s.name, e["name"].get<std::string>(), "sources name", "1");
    check(s.domain, opt_str(e["domain"]), "sources domain", "1");
  }

  // 2. parse vectors (null inputs skipped: string_view has no null)
  for (const auto& e : V["parse"]) {
    if (e["input"].is_null()) continue;
    check_parse(e["input"].get<std::string>(), e);
  }

  // 3. mutations
  {
    const std::vector<std::string> undetected = V["knownUndetected"].get<std::vector<std::string>>();
    std::size_t total = 0;
    for (const auto& seedJ : V["mutationSeeds"]) {
      const std::string s = ci::normalize(seedJ.get<std::string>());
      std::vector<std::string> muts;
      for (std::size_t i = 0; i < 12; ++i)
        for (char d : std::string("0123456789ABCDEF"))
          if (d != s[i]) { std::string m = s; m[i] = d; muts.push_back(m); }
      for (std::size_t i = 0; i < 11; ++i)
        if (s[i] != s[i + 1]) { std::string m = s; std::swap(m[i], m[i + 1]); muts.push_back(m); }
      for (const auto& m : muts) {
        const auto r = ci::parse(m);
        bool known = false;
        for (const auto& u : undetected) known = known || u == m;
        if (known) check(r.status, ci::Status::CienciaId, "undetected mutation", m);
        else if (r.status != ci::Status::Invalid || r.reason != ci::Reason::CheckDigit)
          fail("mutation not detected", m);
      }
      total += muts.size();
    }
    if (total != 2280) { ++failures; std::cerr << "FAIL mutation total " << total << "\n"; }
  }

  // 4-6. computeCheckDigit, normalize, format
  for (const auto& v : V["computeCheckDigit"]) {
    if (v["input"].is_null()) continue;
    const std::string i = v["input"].get<std::string>();
    check(ci::computeCheckDigit(i), opt_str(v["expected"]), "computeCheckDigit", i);
  }
  for (const auto& v : V["normalize"]) {
    if (v["input"].is_null()) continue;
    const std::string i = v["input"].get<std::string>();
    check(ci::normalize(i), v["expected"].get<std::string>(), "normalize", i);
  }
  for (const auto& v : V["format"]) {
    if (v["input"].is_null()) continue;
    const std::string i = v["input"].get<std::string>();
    check(ci::format(i, v["withDomain"].get<bool>()), opt_str(v["expected"]), "format", i);
  }

  if (failures) { std::cerr << failures << " failure(s)\n"; return 1; }
  std::cout << "all vector checks passed\n";
  return 0;
}
