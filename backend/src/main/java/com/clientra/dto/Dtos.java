package com.clientra.dto;

import com.clientra.ai.AiModels;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

/**
 * Every request/response body of the REST API. Records are immutable and their component
 * names are exactly the JSON keys the React app uses (camelCase).
 * Entities are never sent to the browser directly (so password hashes etc. can't leak).
 */
public final class Dtos {
    private Dtos() {}

    // ---------- auth ----------
    public record LoginRequest(@NotBlank(message = "Email is required.") String email,
                               @NotBlank(message = "Password is required.") String password) {}
    public record LoginResponse(String token, UserDto user) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword,
                                        @NotBlank @Size(min = 8, message = "New password must be at least 8 characters.") String newPassword) {}

    // ---------- users ----------
    public record UserDto(Integer userId, Integer companyId, Integer roleId, String role, String fullName,
                          String email, boolean active, Instant createdAt, Instant lastLogin) {}
    /** Slim version for the "assign to" drop-downs (no e-mail / login data). */
    public record AssignableUserDto(Integer userId, String fullName, String role, boolean active) {}
    public record UserRequest(@NotBlank(message = "Name is required.") String fullName,
                              @NotBlank(message = "Email is required.")
                              @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$", message = "Enter a valid email address.") String email,
                              @NotNull(message = "Role is required.") Integer roleId,
                              String password) {}
    public record ActiveRequest(boolean active) {}

    // ---------- leads ----------
    public record LeadRequest(@NotBlank(message = "Name is required.") @Size(max = 120) String fullName,
                              @Pattern(regexp = "^$|^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$", message = "Enter a valid email address.") String email,
                              @Size(max = 30) String phone,
                              @Size(max = 120) String company,
                              @Size(max = 100) String source,
                              Integer stageId,
                              Integer assignedTo) {}
    public record LeadDto(Integer leadId, String fullName, String email, String phone, String company, String source,
                          Integer stageId, String stageName, Integer assignedTo, String ownerName,
                          String priorityLabel, int aiScore, boolean flagged,
                          Instant createdAt, Instant stageChangedAt, Instant lastActivityAt,
                          long idleDays, long daysInStage, double openValue) {}
    public record StageMoveRequest(@NotNull(message = "Stage is required.") Integer stageId) {}
    public record SkippedRow(int row, String reason) {}
    public record ImportResult(int imported, List<SkippedRow> skipped) {}
    public record ExportLogRequest(String format) {}

    // ---------- pipeline ----------
    public record StageNameRequest(@NotBlank(message = "Stage name is required.") @Size(max = 80) String stageName) {}
    public record ReorderRequest(@NotNull List<Integer> ids) {}

    // ---------- activities ----------
    public record ActivityRequest(@NotNull(message = "Lead is required.") Integer leadId,
                                  @NotBlank(message = "Activity type is required.") String type,
                                  @NotBlank(message = "Description is required.") @Size(max = 5000) String description) {}
    public record ActivityDto(Integer activityId, Integer leadId, String leadName, Integer userId, String userName,
                              String type, String description, String sentimentLabel, Instant activityDate) {}
    /** Response of POST /activities: the saved row plus what the AI concluded about it. */
    public record ActivityResult(ActivityDto activity, AiModels.SentimentResult sentiment, boolean flagged,
                                 AiModels.ScoreResult score) {}

    // ---------- deals ----------
    public record DealRequest(@NotNull(message = "Lead is required.") Integer leadId,
                              @NotNull(message = "Amount is required.") @Positive(message = "Amount must be greater than zero.") Double amount,
                              String closingDate, String notes) {}
    public record DealDto(Integer dealId, Integer leadId, double amount, String status, String closingDate,
                          String notes, Instant closedAt) {}
    public record StatusRequest(@NotBlank(message = "Status is required.") String status) {}

    // ---------- reminders ----------
    public record ReminderRequest(@NotNull(message = "Lead is required.") Integer leadId,
                                  @NotBlank(message = "Date and time are required.") String reminderTime,
                                  @NotBlank(message = "Message is required.") @Size(max = 1000) String message,
                                  String priority) {}
    public record ReminderDto(Integer reminderId, Integer leadId, String leadName, Integer userId, String ownerName,
                              Instant reminderTime, String message, String status, String priority) {}

    // ---------- AI ----------
    public record DraftRequest(@NotNull(message = "Lead is required.") Integer leadId, String tone) {}
    public record LeadIdRequest(@NotNull(message = "Lead is required.") Integer leadId) {}
    public record TextRequest(@NotBlank(message = "Text is required.") @Size(max = 5000) String text) {}
    public record AiLogDto(Integer logId, Integer leadId, String requestType, String inputSummary,
                           String aiResponse, Instant timestamp) {}

    // ---------- dashboard & reports ----------
    public record Kpis(long activeLeads, long newThisWeek, double pipelineValue, double conversionRate) {}
    public record StageValue(String stage, double value, long count) {}
    public record TrendPoint(String month, double rate) {}
    public record Insight(String type, String text, Integer leadId) {}   // type: warning | success | info
    public record RepPerformance(String name, long leads, long activities, long won) {}
    public record DashboardDto(Kpis kpis, List<StageValue> valueByStage, List<TrendPoint> conversionTrend,
                               List<LeadDto> topLeads, List<ReminderDto> overdue,
                               List<RepPerformance> repPerformance, List<Insight> insights) {}
    public record NameValue(String name, long value) {}
    public record ReportDto(List<LeadDto> rows, List<NameValue> bySource, List<NameValue> byStage, double totalValue) {}

    // ---------- admin ----------
    public record AuditDto(Integer id, Integer userId, String userName, String action, String entity,
                           String detail, Instant timestamp) {}
    public record RulesDto(boolean emailOnDue, boolean emailOnOverdue, boolean emailOnNegative, boolean weeklyDigest) {}

    // ---------- real-time ----------
    /** Message pushed over the WebSocket. Same shape the notification bell in the UI renders. */
    public record NotificationDto(String id, String title, String body, Integer leadId, Instant at) {}
}
