package com.clientra.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** tbl_leads. aiScore / priorityLabel / flagged are maintained by the AI engine, never typed in by users. */
@Entity
@Table(name = "tbl_leads", indexes = {
        @Index(name = "idx_leads_assigned", columnList = "assigned_to"),
        @Index(name = "idx_leads_stage", columnList = "stage_id"),
        @Index(name = "idx_leads_email", columnList = "email")})
public class Lead {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer leadId;
    @Column(name = "assigned_to")
    private Integer assignedTo;         // FK -> tbl_users.user_id (owner)
    @Column(nullable = false, length = 120)
    private String fullName;
    @Column(length = 120)
    private String email;
    @Column(length = 30)
    private String phone;
    @Column(length = 120)
    private String company;
    @Column(length = 100)
    private String source;
    @Column(name = "stage_id", nullable = false)
    private Integer stageId;            // FK -> tbl_pipeline_stages
    @Column(length = 50)
    private String priorityLabel = "Low";   // High / Medium / Low
    private int aiScore;                // 0-100 conversion likelihood
    private boolean flagged;            // true when the latest interaction was negative (needs review)
    @Column(nullable = false)
    private Instant createdAt = Instant.now();
    private Instant stageChangedAt = Instant.now();

    public Integer getLeadId() { return leadId; }
    public void setLeadId(Integer leadId) { this.leadId = leadId; }
    public Integer getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Integer assignedTo) { this.assignedTo = assignedTo; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Integer getStageId() { return stageId; }
    public void setStageId(Integer stageId) { this.stageId = stageId; }
    public String getPriorityLabel() { return priorityLabel; }
    public void setPriorityLabel(String priorityLabel) { this.priorityLabel = priorityLabel; }
    public int getAiScore() { return aiScore; }
    public void setAiScore(int aiScore) { this.aiScore = aiScore; }
    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getStageChangedAt() { return stageChangedAt; }
    public void setStageChangedAt(Instant stageChangedAt) { this.stageChangedAt = stageChangedAt; }
}
