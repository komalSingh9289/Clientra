package com.clientra.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Tiny CSV reader/writer (RFC-4180 style quoting) so no extra library is needed. */
public final class CsvUtil {
    private CsvUtil() {}

    /**
     * Parses CSV text. The first line is the header. Header names are normalised
     * ("Full Name" -> "fullname") so "Name", "name" and "NAME" all work.
     * Blank lines are skipped. Returns one map per data row.
     */
    public static List<Map<String, String>> parse(String text) {
        List<List<String>> table = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        if (text.startsWith("\uFEFF")) text = text.substring(1);   // Excel's byte-order mark
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') { cell.append('"'); i++; }  // "" = literal quote
                    else quoted = false;
                } else cell.append(c);
            } else if (c == '"') quoted = true;
            else if (c == ',') { row.add(cell.toString()); cell.setLength(0); }
            else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                row.add(cell.toString()); cell.setLength(0);
                addRow(table, row);
                row = new ArrayList<>();
            } else cell.append(c);
        }
        if (cell.length() > 0 || !row.isEmpty()) { row.add(cell.toString()); addRow(table, row); }

        List<Map<String, String>> out = new ArrayList<>();
        if (table.isEmpty()) return out;
        List<String> headers = new ArrayList<>();
        for (String h : table.get(0)) headers.add(h.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", ""));
        for (int r = 1; r < table.size(); r++) {
            Map<String, String> m = new LinkedHashMap<>();
            List<String> cells = table.get(r);
            for (int k = 0; k < headers.size(); k++) m.put(headers.get(k), k < cells.size() ? cells.get(k).trim() : "");
            out.add(m);
        }
        return out;
    }

    private static void addRow(List<List<String>> table, List<String> row) {
        boolean blank = row.stream().allMatch(s -> s.trim().isEmpty());
        if (!blank) table.add(row);
    }

    /** Builds CSV text. Cells that could be run as spreadsheet formulas are neutralised with a leading quote. */
    public static String toCsv(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append(join(headers)).append("\r\n");
        for (List<String> r : rows) sb.append(join(r)).append("\r\n");
        return sb.toString();
    }

    private static String join(List<String> cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(cells.get(i)));
        }
        return sb.toString();
    }

    private static String escape(String v) {
        String s = v == null ? "" : v;
        // CSV-injection guard: Excel treats a leading = + @ as a formula.
        if (!s.isEmpty() && "=+@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
