package com.clientra.web;

import com.clientra.dto.Dtos.AuditDto;
import com.clientra.service.AuditService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** UC-14 View Audit Logs. Administrator only. Read-only: there is deliberately no way to edit or delete entries. */
@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AuditController {
    private final AuditService audit;

    public AuditController(AuditService audit) { this.audit = audit; }

    @GetMapping
    public List<AuditDto> list(@RequestParam(required = false) String action,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return audit.search(action, from, to);
    }
}
