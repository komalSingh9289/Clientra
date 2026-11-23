package com.clientra.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** tbl_deals: monetary opportunity attached to a lead. */
@Entity
@Table(name = "tbl_deals", indexes = @Index(name = "idx_deals_lead", columnList = "lead_id"))
public class Deal {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer dealId;
    @Column(name = "lead_id", nullable = false)
    private Integer leadId;
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private DealStatus status = DealStatus.Open;
    private LocalDate closingDate;      // expected closing date
    @Column(columnDefinition = "TEXT")
    private String notes;
    private Instant closedAt;           // set when the deal becomes Won/Lost; feeds the conversion-rate trend

    public Integer getDealId() { return dealId; }
    public void setDealId(Integer dealId) { this.dealId = dealId; }
    public Integer getLeadId() { return leadId; }
    public void setLeadId(Integer leadId) { this.leadId = leadId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public DealStatus getStatus() { return status; }
    public void setStatus(DealStatus status) { this.status = status; }
    public LocalDate getClosingDate() { return closingDate; }
    public void setClosingDate(LocalDate closingDate) { this.closingDate = closingDate; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }
}
