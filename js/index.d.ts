// Type declarations for @peopleware-pt/ciencia-id.
// Every function accepts any value: non-strings are treated as invalid input.

export type Status = "CIENCIA_ID" | "UNKNOWN_SOURCE" | "INVALID";
export type Reason = "FORMAT" | "CHECK_DIGIT";
export type Warning = "DOMAIN_MISMATCH";

export interface Source {
  code: string;
  known: boolean;
  name: string;
  domain: string | null;
}

export interface ValidParseResult {
  status: "CIENCIA_ID" | "UNKNOWN_SOURCE";
  reason: null;
  warnings: Warning[];
  internalValidation: string;
  source: Source;
  uniqueId: string;
  uniqueIdDecimal: number;
  checkDigit: string;
  indicatedDomain: string | null;
}

export interface InvalidParseResult {
  status: "INVALID";
  reason: Reason;
  warnings: [];
  internalValidation: null;
  source: null;
  uniqueId: null;
  uniqueIdDecimal: null;
  checkDigit: null;
  indicatedDomain: null;
}

export type ParseResult = ValidParseResult | InvalidParseResult;

export declare const SOURCES: Readonly<Record<string, Readonly<{ name: string; domain: string }>>>;
export declare function computeCheckDigit(input: unknown): string | null;
export declare function isValid(input: unknown): boolean;
export declare function parse(input: unknown): ParseResult;
export declare function normalize(input: unknown): string;
export declare function format(input: unknown, withDomain?: boolean): string | null;
