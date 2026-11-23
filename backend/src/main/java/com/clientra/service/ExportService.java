package com.clientra.service;

import com.clientra.dto.Dtos.LeadDto;
import com.clientra.exception.ApiException;
import com.clientra.util.CsvUtil;
import com.clientra.util.TimeUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * CSV and PDF generation for lead lists and reports (UC-10 / UC-11).
 * The PDF uses the built-in Helvetica font, which covers Latin text and "INR" amounts (the rupee
 * sign and Indic scripts would need an embedded font).
 */
@Service
public class ExportService {
    private static final List<String> HEADERS = List.of("Name", "Email", "Phone", "Company", "Source", "Stage",
            "Priority", "AI score", "Assigned to", "Created", "Open value (INR)");

    private List<String> row(LeadDto l) {
        return List.of(nz(l.fullName()), nz(l.email()), nz(l.phone()), nz(l.company()), nz(l.source()), nz(l.stageName()),
                nz(l.priorityLabel()), String.valueOf(l.aiScore()), nz(l.ownerName()),
                l.createdAt().atZone(TimeUtil.ZONE).toLocalDate().toString(), String.format(Locale.ROOT, "%.0f", l.openValue()));
    }

    public String leadsCsv(List<LeadDto> rows) {
        List<List<String>> table = new ArrayList<>();
        for (LeadDto l : rows) table.add(row(l));
        return CsvUtil.toCsv(HEADERS, table);
    }

    public byte[] leadsPdf(String title, List<LeadDto> rows) {
        try {
            Document doc = new Document(PageSize.A4.rotate(), 28, 28, 30, 30);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph(title, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            Font small = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
            doc.add(new Paragraph("Generated " + ZonedDateTime.now(TimeUtil.ZONE).format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm"))
                    + "  |  " + rows.size() + " lead(s)", small));
            doc.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(HEADERS.size());
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            table.setWidths(new float[]{16, 20, 13, 15, 10, 11, 8, 6, 14, 10, 11});
            Font head = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            for (String h : HEADERS) {
                PdfPCell c = new PdfPCell(new Phrase(h, head));
                c.setBackgroundColor(new Color(37, 99, 235));
                c.setPadding(4);
                c.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(c);
            }
            Font body = FontFactory.getFont(FontFactory.HELVETICA, 8);
            for (LeadDto l : rows) {
                for (String v : row(l)) {
                    PdfPCell c = new PdfPCell(new Phrase(v, body));
                    c.setPadding(3);
                    table.addCell(c);
                }
            }
            doc.add(table);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not generate the PDF.");
        }
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
