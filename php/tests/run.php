<?php
declare(strict_types=1);

require __DIR__ . '/../src/CienciaId.php';

use Peopleware\CienciaId\CienciaId;

$V = json_decode(file_get_contents(__DIR__ . '/../../test-vectors.json'), true);
$checks = 0;
$failures = 0;

function check(bool $ok, string $test, $input): void
{
    global $checks, $failures;
    $checks++;
    if (!$ok) {
        $failures++;
        echo 'FAIL ' . $test . ': ' . json_encode($input, JSON_UNESCAPED_UNICODE | JSON_PARTIAL_OUTPUT_ON_ERROR) . "\n";
    }
}

// sources
check(CienciaId::SOURCES['1'] === $V['sources']['1'], 'sources', '1');

// parse vectors
foreach ($V['parse'] as $v) {
    $input = $v['input'];
    unset($v['input']);
    check(CienciaId::parse($input) === $v, 'parse vectors', $input);
    check(CienciaId::isValid($input) === ($v['status'] === 'CIENCIA_ID'), 'parse vectors isValid', $input);
}

// mutations
$total = 0;
foreach ($V['mutationSeeds'] as $seed) {
    $s = CienciaId::normalize($seed);
    $muts = [];
    for ($i = 0; $i < 12; $i++) {
        foreach (str_split('0123456789ABCDEF') as $d) {
            if ($d !== $s[$i]) {
                $muts[] = substr($s, 0, $i) . $d . substr($s, $i + 1);
            }
        }
    }
    for ($i = 0; $i < 11; $i++) {
        if ($s[$i] !== $s[$i + 1]) {
            $muts[] = substr($s, 0, $i) . $s[$i + 1] . $s[$i] . substr($s, $i + 2);
        }
    }
    foreach ($muts as $m) {
        $r = CienciaId::parse($m);
        if (in_array($m, $V['knownUndetected'], true)) {
            check($r['status'] === 'CIENCIA_ID', 'mutations', $m);
        } else {
            check($r['status'] === 'INVALID' && $r['reason'] === 'CHECK_DIGIT', 'mutations', $m);
        }
    }
    $total += count($muts);
}
check($total === 2280, 'mutations total', $total);

// computeCheckDigit vectors
foreach ($V['computeCheckDigit'] as $v) {
    check(CienciaId::computeCheckDigit($v['input']) === $v['expected'], 'computeCheckDigit vectors', $v['input']);
}

// normalize vectors
foreach ($V['normalize'] as $v) {
    check(CienciaId::normalize($v['input']) === $v['expected'], 'normalize vectors', $v['input']);
}

// format vectors
foreach ($V['format'] as $v) {
    check(CienciaId::format($v['input'], $v['withDomain']) === $v['expected'], 'format vectors', $v['input']);
}

echo "$checks checks, $failures failures\n";
exit($failures ? 1 : 0);
