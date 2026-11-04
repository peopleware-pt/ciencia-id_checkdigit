package pt.peopleware.cienciaid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CienciaIdVectorsTest {
    private static JsonObject v;

    @BeforeAll
    static void load() throws IOException {
        String text = new String(Files.readAllBytes(Paths.get("../test-vectors.json")), StandardCharsets.UTF_8);
        v = JsonParser.parseString(text).getAsJsonObject();
    }

    private static String str(JsonElement e) {
        return e.isJsonNull() ? null : e.getAsString();
    }

    private static String str(JsonObject o, String key) {
        return str(o.get(key));
    }

    @Test
    void sources() {
        JsonObject expected = v.getAsJsonObject("sources").getAsJsonObject("1");
        Source s = CienciaId.SOURCES.get("1");
        assertEquals(1, CienciaId.SOURCES.size());
        assertEquals(str(expected, "name"), s.getName());
        assertEquals(str(expected, "domain"), s.getDomain());
        assertEquals("1", s.getCode());
        assertTrue(s.isKnown());
    }

    @Test
    void parseVectors() {
        for (JsonElement el : v.getAsJsonArray("parse")) {
            JsonObject e = el.getAsJsonObject();
            String input = str(e, "input");
            String msg = input;
            ParseResult r = CienciaId.parse(input);
            assertEquals(str(e, "status"), r.getStatus().name(), msg);
            assertEquals(str(e, "reason"), r.getReason() == null ? null : r.getReason().name(), msg);
            List<String> warnings = new ArrayList<>();
            for (JsonElement w : e.getAsJsonArray("warnings")) {
                warnings.add(w.getAsString());
            }
            List<String> actualWarnings = new ArrayList<>();
            for (Warning w : r.getWarnings()) {
                actualWarnings.add(w.name());
            }
            assertEquals(warnings, actualWarnings, msg);
            assertEquals(str(e, "internalValidation"), r.getInternalValidation(), msg);
            if (e.get("source").isJsonNull()) {
                assertNull(r.getSource(), msg);
            } else {
                JsonObject es = e.getAsJsonObject("source");
                Source s = r.getSource();
                assertEquals(str(es, "code"), s.getCode(), msg);
                assertEquals(es.get("known").getAsBoolean(), s.isKnown(), msg);
                assertEquals(str(es, "name"), s.getName(), msg);
                assertEquals(str(es, "domain"), s.getDomain(), msg);
            }
            assertEquals(str(e, "uniqueId"), r.getUniqueId(), msg);
            if (e.get("uniqueIdDecimal").isJsonNull()) {
                assertNull(r.getUniqueIdDecimal(), msg);
            } else {
                assertEquals(Long.valueOf(e.get("uniqueIdDecimal").getAsLong()), r.getUniqueIdDecimal(), msg);
            }
            assertEquals(str(e, "checkDigit"), r.getCheckDigit(), msg);
            assertEquals(str(e, "indicatedDomain"), r.getIndicatedDomain(), msg);
            assertEquals("CIENCIA_ID".equals(str(e, "status")), CienciaId.isValid(input), msg);
        }
    }

    @Test
    void mutations() {
        Set<String> undetected = new HashSet<>();
        for (JsonElement el : v.getAsJsonArray("knownUndetected")) {
            undetected.add(el.getAsString());
        }
        String digits = "0123456789ABCDEF";
        int total = 0;
        for (JsonElement seedEl : v.getAsJsonArray("mutationSeeds")) {
            String s = CienciaId.normalize(seedEl.getAsString());
            List<String> muts = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                for (char d : digits.toCharArray()) {
                    if (d != s.charAt(i)) {
                        muts.add(s.substring(0, i) + d + s.substring(i + 1));
                    }
                }
            }
            for (int i = 0; i < 11; i++) {
                if (s.charAt(i) != s.charAt(i + 1)) {
                    muts.add(s.substring(0, i) + s.charAt(i + 1) + s.charAt(i) + s.substring(i + 2));
                }
            }
            for (String m : muts) {
                ParseResult r = CienciaId.parse(m);
                if (undetected.contains(m)) {
                    assertEquals(Status.CIENCIA_ID, r.getStatus(), m);
                } else {
                    assertEquals(Status.INVALID, r.getStatus(), m);
                    assertEquals(Reason.CHECK_DIGIT, r.getReason(), m);
                }
            }
            total += muts.size();
        }
        assertEquals(2280, total);
    }

    @Test
    void computeCheckDigitVectors() {
        for (JsonElement el : v.getAsJsonArray("computeCheckDigit")) {
            JsonObject e = el.getAsJsonObject();
            String input = str(e, "input");
            assertEquals(str(e, "expected"), CienciaId.computeCheckDigit(input).orElse(null), input);
        }
    }

    @Test
    void normalizeVectors() {
        for (JsonElement el : v.getAsJsonArray("normalize")) {
            JsonObject e = el.getAsJsonObject();
            String input = str(e, "input");
            assertEquals(str(e, "expected"), CienciaId.normalize(input), input);
        }
    }

    @Test
    void formatVectors() {
        for (JsonElement el : v.getAsJsonArray("format")) {
            JsonObject e = el.getAsJsonObject();
            String input = str(e, "input");
            assertEquals(str(e, "expected"),
                    CienciaId.format(input, e.get("withDomain").getAsBoolean()).orElse(null), input);
        }
    }
}
