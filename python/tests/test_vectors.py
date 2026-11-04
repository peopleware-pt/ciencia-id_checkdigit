import json
import unittest
from pathlib import Path

import ciencia_id as C

with open(Path(__file__).resolve().parents[2] / "test-vectors.json", encoding="utf-8") as fh:
    V = json.load(fh)


def to_vector(r):
    return {
        "status": r.status,
        "reason": r.reason,
        "warnings": list(r.warnings),
        "internalValidation": r.internal_validation,
        "source": None if r.source is None else {
            "code": r.source.code, "known": r.source.known,
            "name": r.source.name, "domain": r.source.domain,
        },
        "uniqueId": r.unique_id,
        "uniqueIdDecimal": r.unique_id_decimal,
        "checkDigit": r.check_digit,
        "indicatedDomain": r.indicated_domain,
    }


class VectorsTest(unittest.TestCase):
    def test_sources(self):
        s = C.SOURCES["1"]
        self.assertEqual({"name": s.name, "domain": s.domain}, V["sources"]["1"])

    def test_parse_vectors(self):
        for v in V["parse"]:
            expected = {k: x for k, x in v.items() if k != "input"}
            self.assertEqual(to_vector(C.parse(v["input"])), expected, repr(v["input"]))
            self.assertEqual(C.is_valid(v["input"]), expected["status"] == "CIENCIA_ID", repr(v["input"]))

    def test_mutations(self):
        total = 0
        for seed in V["mutationSeeds"]:
            s = C.normalize(seed)
            muts = []
            for i in range(12):
                for d in "0123456789ABCDEF":
                    if d != s[i]:
                        muts.append(s[:i] + d + s[i + 1:])
            for i in range(11):
                if s[i] != s[i + 1]:
                    muts.append(s[:i] + s[i + 1] + s[i] + s[i + 2:])
            for m in muts:
                r = C.parse(m)
                if m in V["knownUndetected"]:
                    self.assertEqual(r.status, "CIENCIA_ID", m)
                else:
                    self.assertEqual(r.status, "INVALID", m)
                    self.assertEqual(r.reason, "CHECK_DIGIT", m)
            total += len(muts)
        self.assertEqual(total, 2280)

    def test_compute_check_digit_vectors(self):
        for v in V["computeCheckDigit"]:
            self.assertEqual(C.compute_check_digit(v["input"]), v["expected"], repr(v["input"]))

    def test_normalize_vectors(self):
        for v in V["normalize"]:
            self.assertEqual(C.normalize(v["input"]), v["expected"], repr(v["input"]))

    def test_format_vectors(self):
        for v in V["format"]:
            self.assertEqual(C.format(v["input"], v["withDomain"]), v["expected"], repr(v["input"]))

    def test_non_string_input_is_treated_like_none(self):
        self.assertIs(C.is_valid(b"EA1191C239D1"), False)
        self.assertIs(C.is_valid(["EA1191C239D1"]), False)
        self.assertEqual(C.parse(421274525).reason, "FORMAT")
        self.assertEqual(C.normalize(b"ea11"), "")


if __name__ == "__main__":
    unittest.main()
