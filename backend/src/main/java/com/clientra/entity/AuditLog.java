package com.clientra.entity;

import jakarta.persistence.*;
import java.time.Instant;
import org.hibernate.annotations.Immutable;

/**
 * Audit trail (synopsis 11.6). @Immutable = Hibernate never issues an UPDATE for it, and no
 * service or endpoint deletes rows, so the log is append-only.
 */
@Entity
@Immutable
@Table(name = "tbl_audit_logs", indexes = @Index(name = "idx_audit_time", columnList = "logged_at"))
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer userId;             // acting user (null for failed logins / system actions)
    @Column(length = 120)
    private String userName;
    @Column(nullable = false, length = 40)
    private String action;              // LOGIN, LOGOUT, CREATE, UPDATE, DELETE, EXPORT, IMPORT, ROLE_CHANGE, CONFIG
    @Column(length = 120)
    private String entity;              // affected record, e.g. "Lead #12"
    @Column(columnDefinition = "TEXT")
    private String detail;
    @Column(name = "logged_at", nullable = false)
    private Instant timestamp = Instant.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
