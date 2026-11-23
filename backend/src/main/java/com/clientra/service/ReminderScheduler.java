package com.clientra.service;

import com.clientra.dto.Dtos.NotificationDto;
import com.clientra.entity.*;
import com.clientra.repository.*;
import com.clientra.security.Roles;
import com.clientra.ws.NotificationHub;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Background scheduler (Module 6). Runs inside the application, no external cron needed.
 *  1. every 30 s: pending reminders that are now due -> real-time WebSocket push (+ e-mail if enabled)
 *  2. same run: reminders overdue by more than 24 h -> one e-mail nudge
 *  3. every Monday 09:00 IST: weekly digest e-mail to managers/admins (if the admin enabled it)
 * The dueNotified / overdueNotified flags make sure each alert is sent only once.
 */
@Component
public class ReminderScheduler {
    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final ReminderRepository reminders;
    private final LeadRepository leads;
    private final UserRepository users;
    private final RoleRepository roles;
    private final SettingsService settings;
    private final EmailService email;
    private final NotificationHub hub;
    private final long overdueAfterHours;

    public ReminderScheduler(ReminderRepository reminders, LeadRepository leads, UserRepository users, RoleRepository roles,
                             SettingsService settings, EmailService email, NotificationHub hub,
                             @Value("${clientra.scheduler.overdue-after-hours:24}") long overdueAfterHours) {
        this.reminders = reminders; this.leads = leads; this.users = users; this.roles = roles;
        this.settings = settings; this.email = email; this.hub = hub; this.overdueAfterHours = overdueAfterHours;
    }

    @Scheduled(fixedDelayString = "${clientra.scheduler.reminder-poll-ms:30000}", initialDelay = 15000)
    @Transactional
    public void dispatch() {
        Instant now = Instant.now();
        NotificationRules rules = settings.rules();

        // 1. due now
        for (Reminder r : reminders.findByStatusAndDueNotifiedFalseAndReminderTimeLessThanEqual(ReminderStatus.Pending, now)) {
            Lead lead = leads.findById(r.getLeadId()).orElse(null);
            User owner = users.findById(r.getUserId()).orElse(null);
            String leadName = lead == null ? "a lead" : lead.getFullName();
            hub.send(r.getUserId(), new NotificationDto("rem-" + r.getReminderId(), "Follow-up due: " + leadName,
                    r.getMessage(), r.getLeadId(), r.getReminderTime()));
            if (rules.isEmailOnDue() && owner != null) {
                email.send(owner.getEmail(), "Clientra reminder: " + leadName, r.getMessage());
            }
            r.setDueNotified(true);
        }

        // 2. overdue for a long time
        Instant threshold = now.minus(overdueAfterHours, ChronoUnit.HOURS);
        for (Reminder r : reminders.findByStatusAndOverdueNotifiedFalseAndReminderTimeLessThanEqual(ReminderStatus.Pending, threshold)) {
            if (rules.isEmailOnOverdue()) {
                User owner = users.findById(r.getUserId()).orElse(null);
                Lead lead = leads.findById(r.getLeadId()).orElse(null);
                if (owner != null) {
                    email.send(owner.getEmail(), "Clientra: overdue follow-up" + (lead == null ? "" : " for " + lead.getFullName()),
                            "This reminder is overdue: " + r.getMessage());
                }
            }
            r.setOverdueNotified(true);
        }
    }

    @Scheduled(cron = "${clientra.scheduler.digest-cron:0 0 9 * * MON}", zone = "Asia/Kolkata")
    @Transactional(readOnly = true)
    public void weeklyDigest() {
        if (!settings.rules().isWeeklyDigest()) return;
        long total = leads.count();
        long flagged = leads.countByFlaggedTrue();
        long overdue = reminders.findByStatusAndReminderTimeLessThanEqual(ReminderStatus.Pending, Instant.now()).size();
        String body = "Weekly Clientra digest\n\nTotal leads: " + total + "\nLeads with negative sentiment: " + flagged
                + "\nOverdue follow-ups: " + overdue + "\n";
        List<Integer> roleIds = roles.findAll().stream()
                .filter(r -> Roles.ADMIN.equals(r.getRoleName()) || Roles.MANAGER.equals(r.getRoleName()))
                .map(Role::getRoleId).toList();
        for (User u : users.findByRoleIdIn(roleIds)) if (u.isActive()) email.send(u.getEmail(), "Clientra weekly digest", body);
        log.info("Weekly digest sent");
    }
}
