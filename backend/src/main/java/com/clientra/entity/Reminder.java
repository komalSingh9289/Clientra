package com.clientra.entity;

import jakarta.persistence.*;
import java.time.Instant;

/** tbl_reminders: follow-up tasks (Module 6). The two *Notified flags make sure each alert fires once. */
@Entity
@Table(name = "tbl_reminders", indexes = @Index(name = "idx_reminders_user", columnList = "user_id"))
public class Reminder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer reminderId;
    @Column(nullable = false)
    private Integer leadId;
    @Column(name = "user_id", nullable = false)
    private Integer userId;             // reminder owner
    @Column(nullable = false)
    private Instant reminderTime;
    @Column(columnDefinition = "TEXT")
    private String message;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReminderStatus status = ReminderStatus.Pending;
    @Column(length = 10)
    private String priority = "Medium"; // High / Medium / Low
    private boolean dueNotified;        // "due now" push/e-mail already sent
    private boolean overdueNotified;    // "overdue" e-mail already sent

    public Integer getReminderId() { return reminderId; }
    public void setReminderId(Integer reminderId) { this.reminderId = reminderId; }
    public Integer getLeadId() { return leadId; }
    public void setLeadId(Integer leadId) { this.leadId = leadId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Instant getReminderTime() { return reminderTime; }
    public void setReminderTime(Instant reminderTime) { this.reminderTime = reminderTime; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public ReminderStatus getStatus() { return status; }
    public void setStatus(ReminderStatus status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public boolean isDueNotified() { return dueNotified; }
    public void setDueNotified(boolean dueNotified) { this.dueNotified = dueNotified; }
    public boolean isOverdueNotified() { return overdueNotified; }
    public void setOverdueNotified(boolean overdueNotified) { this.overdueNotified = overdueNotified; }
}
