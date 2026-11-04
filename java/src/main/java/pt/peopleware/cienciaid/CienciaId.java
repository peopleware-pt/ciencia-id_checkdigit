package pt.peopleware.cienciaid;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ci\u00eancia ID check digit (ISO/IEC 7064, MOD 17,16).
 *
 * <p>Implements the pseudo-code of &laquo;Estudante-ID &ndash; Regras de uso do Identificador para
 * Parceiros&raquo; v1.0. Every method accepts {@code null}.
 */
public final class CienciaId {
    /** Known sources, keyed by code. */
    public static final Map<String, Source> SOURCES;

    private static final String[] DOMAINS = {"CIENCIA-ID.PT", "ESTUDANTE-ID.PT"};

    static {
        Map<String, Source> m = new LinkedHashMap<String, Source>();
        m.put("1", new Source("1", true, "Ci\u00eancia ID", "ciencia-id.pt"));
        SOURCES = Collections.unmodifiableMap(m);
    }

    private CienciaId() {
    }

    private static boolean isSpace(char c) {
        return c == ' ' || (c >= '\t' && c <= '\r'); // \t \n \u000b \f \r
    }

    private static boolean regionMatches(String s, int at, String prefix) {
        return s.startsWith(prefix, at);
    }

    /** Normalised text and the domain prefix that was stripped, if any. */
    private static String[] split(String input) {
        if (input == null) {
            return new String[] {"", null};
        }
        int start = 0;
        int end = input.length();
        while (start < end && isSpace(input.charAt(start))) {
            start++;
        }
        while (end > start && isSpace(input.charAt(end - 1))) {
            end--;
        }
        StringBuilder up = new StringBuilder(end - start);
        for (int i = start; i < end; i++) {
            char c = input.charAt(i);
            up.append(c >= 'a' && c <= 'z' ? (char) (c - 32) : c);
        }
        String s = up.toString();
        String indicated = null;
        int p = 0;
        if (regionMatches(s, p, "HTTPS://")) {
            p += 8;
        } else if (regionMatches(s, p, "HTTP://")) {
            p += 7;
        }
        if (regionMatches(s, p, "WWW.")) {
            p += 4;
        }
        for (String d : DOMAINS) {
            if (regionMatches(s, p, d) && regionMatches(s, p + d.length(), "/")) {
                indicated = asciiLower(d);
                s = s.substring(p + d.length() + 1);
                break;
            }
        }
        return new String[] {s.replace("-", ""), indicated};
    }

    private static String asciiLower(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c >= 'A' && c <= 'Z' ? (char) (c + 32) : c);
        }
        return b.toString();
    }

    private static int hexValue(char c) {
        return c <= '9' ? c - '0' : c - 'A' + 10;
    }

    private static boolean isHex(String s, int length) {
        if (s.length() != length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F'))) {
                return false;
            }
        }
        return true;
    }

    private static int f(int x) {
        int v = x % 16;
        return v == 0 ? 16 : v;
    }

    private static char checkDigitOf(String s) {
        int t = 16;
        for (int i = 0; i < 11; i++) {
            t = (2 * f(t + hexValue(s.charAt(i)))) % 17;
        }
        return "0123456789ABCDEF".charAt((17 - t) % 16);
    }

    /**
     * Computes the check digit of 11 hexadecimal digits (after {@link #normalize(String)}).
     *
     * @return the check digit, or empty if the input is not 11 hexadecimal digits
     */
    public static Optional<String> computeCheckDigit(String input) {
        String s = normalize(input);
        return isHex(s, 11) ? Optional.of(String.valueOf(checkDigitOf(s))) : Optional.<String>empty();
    }

    /** Whether the input is a well-formed Ci&ecirc;ncia ID with a correct check digit and known source. */
    public static boolean isValid(String input) {
        return parse(input).getStatus() == Status.CIENCIA_ID;
    }

    private static ParseResult invalid(Reason reason) {
        return new ParseResult(Status.INVALID, reason, Collections.<Warning>emptyList(), null, null, null,
                null, null, null);
    }

    /** Parses an identifier; never throws and never returns {@code null}. */
    public static ParseResult parse(String input) {
        String[] parts = split(input);
        String s = parts[0];
        String indicated = parts[1];
        if (!isHex(s, 12)) {
            return invalid(Reason.FORMAT);
        }
        if (checkDigitOf(s) != s.charAt(11)) {
            return invalid(Reason.CHECK_DIGIT);
        }
        String code = s.substring(2, 3);
        Source source = SOURCES.get(code);
        if (source == null) {
            source = new Source(code, false, "Unknown", null);
        }
        List<Warning> warnings = indicated != null && !indicated.equals(source.getDomain())
                ? Collections.singletonList(Warning.DOMAIN_MISMATCH)
                : Collections.<Warning>emptyList();
        String uniqueId = s.substring(3, 11);
        return new ParseResult(source.isKnown() ? Status.CIENCIA_ID : Status.UNKNOWN_SOURCE, null, warnings,
                s.substring(0, 2), source, uniqueId, Long.valueOf(Long.parseLong(uniqueId, 16)),
                s.substring(11), indicated);
    }

    /**
     * Trims ASCII whitespace, upper-cases ASCII letters, strips an optional
     * {@code http(s)://}, {@code www.} and {@code ciencia-id.pt/} or {@code estudante-id.pt/} prefix,
     * and removes every hyphen.
     */
    public static String normalize(String input) {
        return split(input)[0];
    }

    /** Same as {@code format(input, false)}. */
    public static Optional<String> format(String input) {
        return format(input, false);
    }

    /**
     * Formats 12 hexadecimal digits as {@code XXXX-XXXX-XXXX}; the check digit is not verified. With
     * {@code withDomain} and a known source, the source domain and a slash are prepended.
     *
     * @return the formatted text, or empty if the input is not 12 hexadecimal digits
     */
    public static Optional<String> format(String input, boolean withDomain) {
        String s = normalize(input);
        if (!isHex(s, 12)) {
            return Optional.<String>empty();
        }
        String text = s.substring(0, 4) + "-" + s.substring(4, 8) + "-" + s.substring(8);
        Source source = SOURCES.get(s.substring(2, 3));
        return Optional.of(withDomain && source != null ? source.getDomain() + "/" + text : text);
    }
}
