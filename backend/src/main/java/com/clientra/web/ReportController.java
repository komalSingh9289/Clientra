package com.clientra.web;

import com.clientra.dto.Dtos.ReportDto;
import com.clientra.service.AuditService;
import com.clientra.service.ExportService;
import com.clientra.service.LeadService;
import com.clientra.service.ReportService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** UC-10 Generate Report / UC-11 Export. Sales Managers and Administrators only. */
@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasAnyRole('ADMINISTRATOR','SALES_MANAGER')")
public class ReportController {
    private final ReportService reports;
    private final ExportService export;
    private final AuditService audit;

    public ReportController(ReportService reports, ExportService export, AuditService audit) {
        this.reports = reports; this.export = export; this.audit = audit;
    }

    /** GET /api/reports?from=&to=&stageId=&source=&assignedTo= */
    @GetMapping
    public ReportDto report(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            @RequestParam(required = false) Integer stageId,
                            @RequestParam(required = false) String source,
                            @RequestParam(required = false) Integer assignedTo) {
        return reports.build(new LeadService.Filter(null, stageId, null, source, assignedTo, from, to));
    }

    /** GET /api/reports/export?format=csv|pdf&... */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportReport(@RequestParam(defaultValue = "csv") String format,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                               @RequestParam(required = false) Integer stageId,
                                               @RequestParam(required = false) String source,
                                               @RequestParam(required = false) Integer assignedTo) {
        ReportDto r = reports.build(new LeadService.Filter(null, stageId, null, source, assignedTo, from, to));
        audit.log("EXPORT", "Report", format.toUpperCase() + " report export (" + r.rows().size() + " rows)");
        return LeadController.file(format, "clientra-report", "Clientra report", r.rows(), export);
    }
}
