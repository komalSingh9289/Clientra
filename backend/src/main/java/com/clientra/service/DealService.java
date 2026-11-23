package com.clientra.service;

import com.clientra.dto.Dtos.DealDto;
import com.clientra.dto.Dtos.DealRequest;
import com.clientra.entity.Deal;
import com.clientra.entity.DealStatus;
import com.clientra.entity.Lead;
import com.clientra.exception.ApiException;
import com.clientra.repository.DealRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Deals attached to a lead. Every change re-scores the lead (deal value is one of the 4 AI signals). */
@Service
public class DealService {
    private final DealRepository deals;
    private final LeadAccess access;
    private final AiService ai;
    private final AuditService audit;

    public DealService(DealRepository deals, LeadAccess access, AiService ai, AuditService audit) {
        this.deals = deals;
        this.access = access;
        this.ai = ai;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<DealDto> byLead(Integer leadId) {
        access.require(leadId);
        List<DealDto> out = new ArrayList<>();
        for (Deal d : deals.findByLeadId(leadId)) out.add(toDto(d));
        return out;
    }

    @Transactional
    public DealDto create(DealRequest r) {
        Lead lead = access.require(r.leadId());
        Deal d = new Deal();
        d.setLeadId(lead.getLeadId());
        d.setAmount(BigDecimal.valueOf(r.amount()));
        d.setClosingDate(parseDate(r.closingDate()));
        d.setNotes(r.notes() == null || r.notes().isBlank() ? null : r.notes().trim());
        d.setStatus(DealStatus.Open);
        deals.save(d);
        ai.rescore(lead);
        audit.log("CREATE", "Deal #" + d.getDealId(), "Deal of " + d.getAmount() + " for " + lead.getFullName());
        return toDto(d);
    }

    @Transactional
    public DealDto setStatus(Integer id, String status) {
        Deal d = deals.findById(id).orElseThrow(() -> ApiException.notFound("Deal not found."));
        Lead lead = access.require(d.getLeadId());
        DealStatus s;
        try {
            s = DealStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Deal status must be Open, Won or Lost.");
        }
        if (s != d.getStatus()) {
            d.setStatus(s);
            d.setClosedAt(s == DealStatus.Open ? null : Instant.now());   // closing time feeds the conversion trend
            deals.save(d);
            ai.rescore(lead);
            audit.log("UPDATE", "Deal #" + id, "Deal status set to " + s + " for " + lead.getFullName());
        }
        return toDto(d);
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("Closing date must look like 2026-12-31.");
        }
    }

    private DealDto toDto(Deal d) {
        return new DealDto(d.getDealId(), d.getLeadId(), d.getAmount().doubleValue(), d.getStatus().name(),
                d.getClosingDate() == null ? null : d.getClosingDate().toString(), d.getNotes(), d.getClosedAt());
    }
}
