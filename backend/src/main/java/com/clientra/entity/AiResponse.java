package com.clientra.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** tbl_ai_responses: structured result (score, summary, recommendations) of an AiLog entry. */
@Entity
@Table(name = "tbl_ai_responses")
public class AiResponse {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer responseId;
    @Column(nullable = false)
    private Integer logId;
    private Double insightsScore;
    @Column(columnDefinition = "TEXT")
    private String summary;
    @Column(columnDefinition = "TEXT")
    private String recommendations;
    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public Integer getResponseId() { return responseId; }
    public void setResponseId(Integer responseId) { this.responseId = responseId; }
    public Integer getLogId() { return logId; }
    public void setLogId(Integer logId) { this.logId = logId; }
    public Double getInsightsScore() { return insightsScore; }
    public void setInsightsScore(Double insightsScore) { this.insightsScore = insightsScore; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getRecommendations() { return recommendations; }
    public void setRecommendations(String recommendations) { this.recommendations = recommendations; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
