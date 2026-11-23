package com.clientra.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** tbl_activities: every call / email / meeting / note logged against a lead (Module 4). */
@Entity
@Table(name = "tbl_activities", indexes = @Index(name = "idx_activities_lead", columnList = "lead_id"))
public class Activity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer activityId;
    @Column(name = "lead_id", nullable = false)
    private Integer leadId;
    @Column(nullable = false)
    private Integer userId;             // who logged it
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ActivityType type;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(length = 50)
    private String sentimentLabel;      // Positive / Neutral / Negative, set by the AI on save
    @Column(nullable = false)
    private Instant activityDate = Instant.now();

    public Integer getActivityId() { return activityId; }
    public void setActivityId(Integer activityId) { this.activityId = activityId; }
    public Integer getLeadId() { return leadId; }
    public void setLeadId(Integer leadId) { this.leadId = leadId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public ActivityType getType() { return type; }
    public void setType(ActivityType type) { this.type = type; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSentimentLabel() { return sentimentLabel; }
    public void setSentimentLabel(String sentimentLabel) { this.sentimentLabel = sentimentLabel; }
    public Instant getActivityDate() { return activityDate; }
    public void setActivityDate(Instant activityDate) { this.activityDate = activityDate; }
}
