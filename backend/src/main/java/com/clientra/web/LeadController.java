package com.clientra.web;

import com.clientra.dto.Dtos.*;
import com.clientra.service.AuditService;
import com.clientra.service.ExportService;
import com.clientra.service.LeadService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** UC-02 Add/Edit Lead, UC-03 View Pipeline (stage change), lead import/export. All roles; data scope is applied in the service. */
@RestController
@RequestMapping("/api/leads")
public class LeadController {
    private final LeadService leads;
    private final ExportService export;
    private final AuditService audit;

    public LeadController(LeadService leads, ExportService export, AuditService audit) {
        this.leads = leads; this.export = export; this.audit = audit;
    }

    /** GET /api/leads?search=&stageId=&priority=&source=&assignedTo=&from=&to= */
    @GetMapping
    public List<LeadDto> list(@RequestParam(required = false) String search,
                              @RequestParam(required = false) Integer stageId,
                              @RequestParam(required = false) String priority,
                              @RequestParam(required = false) String source,
                              @RequestParam(required = false) Integer assignedTo,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return leads.list(new LeadService.Filter(search, stageId, priority, source, assignedTo, from, to));
    }

    /** GET /api/leads/export?format=csv|pdf (same filters as the list) : server-side file download (UC-11). */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportLeads(@RequestParam(defaultValue = "csv") String format,
                                              @RequestParam(required = false) String search,
                                              @RequestParam(required = false) Integer stageId,
                                              @RequestParam(required = false) String priority,
                                              @RequestParam(required = false) String source,
                                              @RequestParam(required = false) Integer assignedTo,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<LeadDto> rows = leads.list(new LeadService.Filter(search, stageId, priority, source, assignedTo, from, to));
        audit.log("EXPORT", "Leads", format.toUpperCase() + " export of " + rows.size() + " lead(s)");
        return file(format, "clientra-leads", "Clientra leads", rows, export);
    }

    /** POST /api/leads/export-log {format} : the browser built the file itself, we just record that an export happened. */
    @PostMapping("/export-log")
    public ResponseEntity<Void> exportLog(@RequestBody ExportLogRequest r) {
        audit.log("EXPORT", "Leads", (r.format() == null ? "File" : r.format()) + " export");
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public LeadDto get(@PathVariable Integer id) { return leads.get(id); }

    /** POST /api/leads[?force=true] : 409 {code:"DUPLICATE"} when e-mail/phone exists, unless force=true. */
    @PostMapping
    public ResponseEntity<LeadDto> create(@Valid @RequestBody LeadRequest r, @RequestParam(defaultValue = "false") boolean force) {
        return ResponseEntity.status(201).body(leads.create(r, force));
    }

    @PutMapping("/{id}")
    public LeadDto update(@PathVariable Integer id, @Valid @RequestBody LeadRequest r) { return leads.update(id, r); }

    /** PATCH /api/leads/{id}/stage {stageId} : kanban drag-and-drop. */
    @PatchMapping("/{id}/stage")
    public LeadDto stage(@PathVariable Integer id, @Valid @RequestBody StageMoveRequest r) { return leads.updateStage(id, r.stageId()); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        leads.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** POST /api/leads/import : JSON rows parsed in the browser. */
    @PostMapping("/import")
    public ImportResult importRows(@RequestBody List<Map<String, String>> rows) { return leads.bulkImport(rows); }

    /** POST /api/leads/import/csv : multipart upload, field name "file" (handy for Postman / scripts). */
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResult importCsv(@RequestParam("file") MultipartFile file) { return leads.importCsv(file); }

    /** Builds the download response for csv or pdf (shared with ReportController). */
    static ResponseEntity<byte[]> file(String format, String baseName, String title, List<LeadDto> rows, ExportService export) {
        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] bytes = pdf ? export.leadsPdf(title, rows) : export.leadsCsv(rows).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + baseName + (pdf ? ".pdf" : ".csv") + "\"")
                .contentType(pdf ? MediaType.APPLICATION_PDF : MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }
}
