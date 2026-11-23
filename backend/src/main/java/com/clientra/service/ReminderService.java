package com.clientra.service;

import com.clientra.dto.Dtos.ReminderDto;
import com.clientra.dto.Dtos.ReminderRequest;
import com.clientra.entity.*;
import com.clientra.exception.ApiException;
import com.clientra.repository.LeadRepository;
import com.clientra.repository.ReminderRepository;
import com.clientra.repository.UserRepository;
import com.clientra.security.AuthUser;
import com.clientra.security.CurrentUser;
import com.clientra.util.TimeUtil;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Module 6 (Reminders): create follow-ups, list "My Tasks", mark Completed / Cancelled.
 * The alerts themselves (WebSocket push + e-mail) are sent by ReminderScheduler.
 */
@Service
public class ReminderService {
    private static final Set<String> PRIORITIES = Set.of("High", "Medium", "Low");

    private final ReminderRepository reminders;
    private final LeadRepository leads;
    private final UserRepository users;
    private final LeadAccess access;
    private final CurrentUser current;

    public ReminderService(ReminderRepository reminders, LeadRepository leads, UserRepository users,
                           LeadAccess access, CurrentUser current) {
        this.reminders = reminders; this.leads = leads; this.users = users; this.access = access; this.current = current;
    }

    /** Reps see their own tasks; managers and admins see the whole team's. */
    @Transactional(readOnly = true)
    public List<ReminderDto> list() {
        AuthUser me = current.get();
        return toDtos(me.isRep() ? reminders.findByUserIdOrderByReminderTimeAsc(me.userId())
                                 : reminders.findAllByOrderByReminderTimeAsc());
    }

    @Transactional
    public ReminderDto create(ReminderRequest r) {
        Lead lead = access.require(r.leadId());
        Reminder x = new Reminder();
        x.setLeadId(lead.getLeadId());
        x.setUserId(current.id());
        x.setReminderTime(parseTime(r.reminderTime()));
        x.setMessage(r.message().trim());
        String p = r.priority() == null ? "Medium" : r.priority();
        x.setPriority(PRIORITIES.contains(p) ? p : "Medium");
        x.setStatus(ReminderStatus.Pending);
        reminders.save(x);
        return toDtos(List.of(x)).get(0);
    }

    @Transactional
    public ReminderDto setStatus(Integer id, String status) {
        Reminder x = reminders.findById(id).orElseThrow(() -> ApiException.notFound("Reminder not found."));
        AuthUser me = current.get();
        if (me.isRep() && !x.getUserId().equals(me.userId())) throw ApiException.forbidden("You can only change your own reminders.");
        try {
            x.setStatus(ReminderStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Status must be Pending, Completed or Cancelled.");
        }
        reminders.save(x);
        return toDtos(List.of(x)).get(0);
    }

    /** Accepts "2026-10-14T10:00:00Z" (what the React app sends) or a local "2026-10-14T10:00". */
    private Instant parseTime(String s) {
        try {
            return Instant.parse(s.trim());
        } catch (DateTimeParseException e1) {
            try {
                return LocalDateTime.parse(s.trim()).atZone(TimeUtil.ZONE).toInstant();
            } catch (DateTimeParseException e2) {
                throw ApiException.badRequest("Enter a valid date and time.");
            }
        }
    }

    /** Adds lead and owner names (bulk-loaded, no N+1). */
    public List<ReminderDto> toDtos(List<Reminder> rows) {
        Set<Integer> leadIds = new HashSet<>(), userIds = new HashSet<>();
        for (Reminder r : rows) { leadIds.add(r.getLeadId()); userIds.add(r.getUserId()); }
        Map<Integer, String> leadNames = new HashMap<>(), userNames = new HashMap<>();
        for (Lead l : leads.findAllById(leadIds)) leadNames.put(l.getLeadId(), l.getFullName());
        for (User u : users.findAllById(userIds)) userNames.put(u.getUserId(), u.getFullName());
        List<ReminderDto> out = new ArrayList<>();
        for (Reminder r : rows) {
            out.add(new ReminderDto(r.getReminderId(), r.getLeadId(), leadNames.getOrDefault(r.getLeadId(), ""),
                    r.getUserId(), userNames.getOrDefault(r.getUserId(), ""), r.getReminderTime(), r.getMessage(),
                    r.getStatus().name(), r.getPriority()));
        }
        return out;
    }
}
