package com.clientra.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** tbl_ai_logs: one row per AI request (audit of what the AI was asked and what it answered). */
@Entity
@Table(name = "tbl_ai_logs")
public class AiLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer logId;
    private Integer leadId;             // null for lead-less requests (e.g. the sentiment tester)
    @Column(length = 100)
    private String requestType;         // SENTIMENT, LEAD_SCORE, MESSAGE_DRAFT, SUMMARY
    @Column(columnDefinition = "TEXT")
    private String inputSummary;
    @Column(columnDefinition = "TEXT")
    private String aiResponse;
    @Column(name = "logged_at", nullable = false)   // 'timestamp' is a SQL keyword, so the column is renamed
    private Instant timestamp = Instant.now();

    public Integer getLogId() { return logId; }
    public void setLogId(Integer logId) { this.logId = logId; }
    public Integer getLeadId() { return leadId; }
    public void setLeadId(Integer leadId) { this.leadId = leadId; }
    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }
    public String getInputSummary() { return inputSummary; }
    public void setInputSummary(String inputSummary) { this.inputSummary = inputSummary; }
    public String getAiResponse() { return aiResponse; }
    public void setAiResponse(String aiResponse) { this.aiResponse = aiResponse; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
