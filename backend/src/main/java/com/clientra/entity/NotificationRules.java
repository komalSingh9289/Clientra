package com.clientra.entity;

import jakarta.persistence.*;

/** tbl_notification_rules: single row (id = 1) holding the e-mail rules configured by the admin (Module 8). */
@Entity
@Table(name = "tbl_notification_rules")
public class NotificationRules {
    @Id
    private Integer id = 1;
    private boolean emailOnDue = true;
    private boolean emailOnOverdue = true;
    private boolean emailOnNegative = true;
    private boolean weeklyDigest = false;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public boolean isEmailOnDue() { return emailOnDue; }
    public void setEmailOnDue(boolean emailOnDue) { this.emailOnDue = emailOnDue; }
    public boolean isEmailOnOverdue() { return emailOnOverdue; }
    public void setEmailOnOverdue(boolean emailOnOverdue) { this.emailOnOverdue = emailOnOverdue; }
    public boolean isEmailOnNegative() { return emailOnNegative; }
    public void setEmailOnNegative(boolean emailOnNegative) { this.emailOnNegative = emailOnNegative; }
    public boolean isWeeklyDigest() { return weeklyDigest; }
    public void setWeeklyDigest(boolean weeklyDigest) { this.weeklyDigest = weeklyDigest; }
}
