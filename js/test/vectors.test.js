"use strict";
const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const C = require("../index.js");
const V = JSON.parse(fs.readFileSync(path.join(__dirname, "..", "..", "test-vectors.json"), "utf8"));

test("sources", () => assert.deepEqual({ ...C.SOURCES["1"] }, V.sources["1"]));

test("parse vectors", () => {
  for (const { input, ...expected } of V.parse) {
    assert.deepEqual(C.parse(input), expected, JSON.stringify(input));
    assert.equal(C.isValid(input), expected.status === "CIENCIA_ID", JSON.stringify(input));
  }
});

test("mutations", () => {
  let total = 0;
  for (const seed of V.mutationSeeds) {
    const s = C.normalize(seed), muts = [];
    for (let i = 0; i < 12; i++) for (const d of "0123456789ABCDEF") if (d !== s[i]) muts.push(s.slice(0, i) + d + s.slice(i + 1));
    for (let i = 0; i < 11; i++) if (s[i] !== s[i + 1]) muts.push(s.slice(0, i) + s[i + 1] + s[i] + s.slice(i + 2));
    for (const m of muts) {
      const r = C.parse(m);
      if (V.knownUndetected.includes(m)) assert.equal(r.status, "CIENCIA_ID", m);
      else { assert.equal(r.status, "INVALID", m); assert.equal(r.reason, "CHECK_DIGIT", m); }
    }
    total += muts.length;
  }
  assert.equal(total, 2280);
});

test("computeCheckDigit vectors", () => { for (const v of V.computeCheckDigit) assert.equal(C.computeCheckDigit(v.input), v.expected, JSON.stringify(v.input)); });
test("normalize vectors", () => { for (const v of V.normalize) assert.equal(C.normalize(v.input), v.expected, JSON.stringify(v.input)); });
test("format vectors", () => { for (const v of V.format) assert.equal(C.format(v.input, v.withDomain), v.expected, JSON.stringify(v.input)); });

test("linear trim", { timeout: 2000 }, () => {
  C.parse("x" + " ".repeat(1e6) + "x");
  C.normalize(" ".repeat(1e6) + "x" + " ".repeat(1e6));
});

test("non-string input is treated like null", () => {
  assert.equal(C.isValid(["EA1191C239D1"]), false);
  assert.equal(C.isValid(Buffer.from("EA1191C239D1")), false);
  assert.equal(C.isValid(421274525), false);
  const r = C.parse(Object.create(null));
  assert.equal(r.status, "INVALID");
  assert.equal(r.reason, "FORMAT");
  assert.equal(C.normalize(["ea11"]), "");
});
