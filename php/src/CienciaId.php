<?php
declare(strict_types=1);

namespace Peopleware\CienciaId;

/**
 * Ciência ID check digit (ISO/IEC 7064, MOD 17,16).
 *
 * Implements the pseudo-code of «Estudante-ID – Regras de uso do Identificador para Parceiros» v1.0.
 */
final class CienciaId
{
    public const SOURCES = ['1' => ['name' => 'Ciência ID', 'domain' => 'ciencia-id.pt']];

    private const UPPER = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    private const LOWER = 'abcdefghijklmnopqrstuvwxyz';
    private const WHITESPACE = " \t\n\r\f\v";
    private const DOMAIN_RE = '~\A(?:HTTPS?://)?(?:WWW\.)?((?:CIENCIA|ESTUDANTE)-ID\.PT)/~';

    private function __construct()
    {
    }

    /**
     * Splits input into the canonical upper-case ID text and the domain prefix found, if any.
     * Only ASCII whitespace is trimmed and only ASCII letters are case-mapped (locale independent).
     *
     * @return array{0: string, 1: ?string}
     */
    private static function split(?string $input): array
    {
        if ($input === null) {
            return ['', null];
        }
        $s = strtr(trim($input, self::WHITESPACE), self::LOWER, self::UPPER);
        $indicatedDomain = null;
        if (preg_match(self::DOMAIN_RE, $s, $m) === 1) {
            $indicatedDomain = strtr($m[1], self::UPPER, self::LOWER);
            $s = substr($s, strlen($m[0]));
        }
        return [str_replace('-', '', $s), $indicatedDomain];
    }

    private static function f(int $x): int
    {
        $v = $x % 16;
        return $v === 0 ? 16 : $v;
    }

    private static function checkDigitOf(string $s): string
    {
        $t = 16;
        for ($i = 0; $i < 11; $i++) {
            $t = (2 * self::f($t + hexdec($s[$i]))) % 17;
        }
        return strtr(dechex((17 - $t) % 16), self::LOWER, self::UPPER);
    }

    private static function sourceOf(string $code): array
    {
        if (isset(self::SOURCES[$code])) {
            return [
                'code' => $code,
                'known' => true,
                'name' => self::SOURCES[$code]['name'],
                'domain' => self::SOURCES[$code]['domain'],
            ];
        }
        return ['code' => $code, 'known' => false, 'name' => 'Unknown', 'domain' => null];
    }

    private static function invalid(string $reason): array
    {
        return [
            'status' => 'INVALID',
            'reason' => $reason,
            'warnings' => [],
            'internalValidation' => null,
            'source' => null,
            'uniqueId' => null,
            'uniqueIdDecimal' => null,
            'checkDigit' => null,
            'indicatedDomain' => null,
        ];
    }

    public static function normalize(?string $id): string
    {
        return self::split($id)[0];
    }

    /** Returns the check digit for 11 hex digits, or null if the input is malformed. */
    public static function computeCheckDigit(?string $id): ?string
    {
        $s = self::normalize($id);
        return preg_match('/\A[0-9A-F]{11}\z/', $s) === 1 ? self::checkDigitOf($s) : null;
    }

    public static function parse(?string $id): array
    {
        [$s, $indicatedDomain] = self::split($id);
        if (preg_match('/\A[0-9A-F]{12}\z/', $s) !== 1) {
            return self::invalid('FORMAT');
        }
        if (self::checkDigitOf($s) !== $s[11]) {
            return self::invalid('CHECK_DIGIT');
        }
        $source = self::sourceOf($s[2]);
        $uniqueId = substr($s, 3, 8);
        return [
            'status' => $source['known'] ? 'CIENCIA_ID' : 'UNKNOWN_SOURCE',
            'reason' => null,
            'warnings' => ($indicatedDomain !== null && $indicatedDomain !== $source['domain']) ? ['DOMAIN_MISMATCH'] : [],
            'internalValidation' => substr($s, 0, 2),
            'source' => $source,
            'uniqueId' => $uniqueId,
            'uniqueIdDecimal' => (int) hexdec($uniqueId),
            'checkDigit' => $s[11],
            'indicatedDomain' => $indicatedDomain,
        ];
    }

    public static function isValid(?string $id): bool
    {
        return self::parse($id)['status'] === 'CIENCIA_ID';
    }

    /** Formats 12 hex digits as XXXX-XXXX-XXXX (the check digit is NOT verified); null if malformed. */
    public static function format(?string $id, bool $withDomain = false): ?string
    {
        $s = self::normalize($id);
        if (preg_match('/\A[0-9A-F]{12}\z/', $s) !== 1) {
            return null;
        }
        $text = substr($s, 0, 4) . '-' . substr($s, 4, 4) . '-' . substr($s, 8, 4);
        $source = self::sourceOf($s[2]);
        return ($withDomain && $source['known']) ? $source['domain'] . '/' . $text : $text;
    }
}
