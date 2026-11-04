"use strict";
// Ciência ID check digit (ISO/IEC 7064, MOD 17,16).
// Implements the pseudo-code of «Estudante-ID – Regras de uso do Identificador para Parceiros» v1.0.

const SOURCES = Object.freeze({
  "1": Object.freeze({ name: "Ciência ID", domain: "ciencia-id.pt" }),
});

const DOMAIN_RE = /^(?:HTTPS?:\/\/)?(?:WWW\.)?((?:CIENCIA|ESTUDANTE)-ID\.PT)\//;

function f(x) {
  const v = x % 16;
  return v === 0 ? 16 : v;
}

// Returns { s, indicatedDomain }: the canonical upper-case ID text and the domain prefix found, if any.
// Only ASCII whitespace is trimmed and only ASCII letters are upper-cased (no Unicode folding).
function split(input) {
  if (input === null || input === undefined) return { s: "", indicatedDomain: null };
  let s = String(input)
    .replace(/^[ \t\n\r\f\v]+|[ \t\n\r\f\v]+$/g, "")
    .replace(/[a-z]/g, (c) => String.fromCharCode(c.charCodeAt(0) - 32));
  let indicatedDomain = null;
  const m = DOMAIN_RE.exec(s);
  if (m) {
    indicatedDomain = m[1].toLowerCase();
    s = s.slice(m[0].length);
  }
  return { s: s.replace(/-/g, ""), indicatedDomain };
}

function normalize(input) {
  return split(input).s;
}

function checkDigitOf(s) {
  let t = 16;
  for (let i = 0; i < 11; i++) t = (2 * f(t + parseInt(s[i], 16))) % 17;
  return ((17 - t) % 16).toString(16).toUpperCase();
}

function computeCheckDigit(input) {
  const s = normalize(input);
  return /^[0-9A-F]{11}$/.test(s) ? checkDigitOf(s) : null;
}

function sourceOf(code) {
  const known = Object.prototype.hasOwnProperty.call(SOURCES, code);
  return known
    ? { code, known: true, name: SOURCES[code].name, domain: SOURCES[code].domain }
    : { code, known: false, name: "Unknown", domain: null };
}

function invalid(reason) {
  return {
    status: "INVALID", reason, warnings: [], internalValidation: null, source: null,
    uniqueId: null, uniqueIdDecimal: null, checkDigit: null, indicatedDomain: null,
  };
}

function parse(input) {
  const { s, indicatedDomain } = split(input);
  if (!/^[0-9A-F]{12}$/.test(s)) return invalid("FORMAT");
  if (checkDigitOf(s) !== s[11]) return invalid("CHECK_DIGIT");
  const source = sourceOf(s[2]);
  const uniqueId = s.slice(3, 11);
  return {
    status: source.known ? "CIENCIA_ID" : "UNKNOWN_SOURCE",
    reason: null,
    warnings: indicatedDomain !== null && indicatedDomain !== source.domain ? ["DOMAIN_MISMATCH"] : [],
    internalValidation: s.slice(0, 2),
    source,
    uniqueId,
    uniqueIdDecimal: parseInt(uniqueId, 16),
    checkDigit: s[11],
    indicatedDomain,
  };
}

function isValid(input) {
  return parse(input).status === "CIENCIA_ID";
}

// Formats 12 hex digits as XXXX-XXXX-XXXX (the check digit is NOT verified).
function format(input, withDomain = false) {
  const s = normalize(input);
  if (!/^[0-9A-F]{12}$/.test(s)) return null;
  const text = `${s.slice(0, 4)}-${s.slice(4, 8)}-${s.slice(8)}`;
  const source = sourceOf(s[2]);
  return withDomain && source.known ? `${source.domain}/${text}` : text;
}

module.exports = { SOURCES, computeCheckDigit, isValid, parse, normalize, format };
