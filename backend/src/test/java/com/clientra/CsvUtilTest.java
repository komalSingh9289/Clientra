package com.clientra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clientra.util.CsvUtil;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Unit test: CSV import parsing and the CSV-injection guard on export. */
class CsvUtilTest {
    @Test void parsesQuotedFieldsBlankLinesAndNormalisesHeaders() {
        List<Map<String, String>> rows = CsvUtil.parse("\uFEFFFull Name,Email,Company\r\n\"Doe, John\",j@x.in,\"Acme \"\"Inc\"\"\"\r\n\r\nAnn,a@b.in,Z\r\n");
        assertEquals(2, rows.size());
        assertEquals("Doe, John", rows.get(0).get("fullname"));
        assertEquals("Acme \"Inc\"", rows.get(0).get("company"));
        assertEquals("a@b.in", rows.get(1).get("email"));
    }

    @Test void exportNeutralisesFormulasAndQuotesCommas() {
        assertEquals("A,B\r\n'=cmd(),\"x,y\"\r\n", CsvUtil.toCsv(List.of("A", "B"), List.of(List.of("=cmd()", "x,y"))));
    }
}
