package com.clientra.service;

import com.clientra.ai.AiModels.ScoreResult;
import com.clientra.ai.AiModels.SentimentResult;
import com.clientra.dto.Dtos.ActivityDto;
import com.clientra.dto.Dtos.ActivityRequest;
import com.clientra.dto.Dtos.ActivityResult;
import com.clientra.dto.Dtos.NotificationDto;
import com.clientra.entity.*;
import com.clientra.exception.ApiException;
import com.clientra.repository.ActivityRepository;
import com.clientra.repository.LeadRepository;
import com.clientra.repository.UserRepository;
import com.clientra.security.AuthUser;
import com.clientra.security.CurrentUser;
import com.clientra.ws.NotificationHub;
import java.time.Instant;
import java.util.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Module 4 (Activity Logging). Flow of create() - this is the heart of the AI loop (diagram 7.5):
 *   1. check the caller may touch the lead
 *   2. AI sentiment analysis of the note
 *   3. save the activity with its sentiment label
 *   4. flag the lead if the sentiment is Negative (priority review)
 *   5. re-score the lead (ML scoring) so priority reflects the new interaction
 *   6. if Negative: alert the lead's owner (real-time push + optional e-mail, per the admin's rules)
 */
@Service
public class ActivityService {
    private final ActivityRepository activities;
    private final LeadRepository leads;
    private final UserRepository users;
    private final LeadAccess access;
    private final AiService ai;
    private final SettingsService settings;
    private final EmailService email;
    private final NotificationHub hub;
    private final CurrentUser current;

    public ActivityService(ActivityRepository activities, LeadRepository leads, UserRepository users, LeadAccess access,
                           AiService ai, SettingsService settings, EmailService email, NotificationHub hub, CurrentUser current) {
        this.activities = activities; this.leads = leads; this.users = users; this.access = access;
        this.ai = ai; this.settings = settings; this.email = email; this.hub = hub; this.current = current;
    }

    /** GET /activities?leadId= : one lead's history, or (no leadId) the latest 1000 the caller may see. */
    @Transactional(readOnly = true)
    public List<ActivityDto> list(Integer leadId) {
        AuthUser me = current.get();
        List<Activity> rows;
        if (leadId != null) {
            access.require(leadId);
            rows = activities.findByLeadIdOrderByActivityDateDesc(leadId);
        } else if (me.isRep()) {
            rows = activities.findForOwner(me.userId(), PageRequest.of(0, 1000));
        } else {
            rows = activities.findAllByOrderByActivityDateDesc(PageRequest.of(0, 1000));
        }
        return toDtos(rows);
    }

    @Transactional
    public ActivityResult create(ActivityRequest r) {
        Lead lead = access.require(r.leadId());
        AuthUser me = current.get();
        ActivityType type;
        try {
            type = ActivityType.valueOf(r.type());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Activity type must be Call, Email, Meeting or Note.");
        }
        String text = r.description().trim();

        SentimentResult s = ai.analyzeForActivity(lead.getLeadId(), text);         // 2
        Activity a = new Activity();                                                // 3
        a.setLeadId(lead.getLeadId());
        a.setUserId(me.userId());
        a.setType(type);
        a.setDescription(text);
        a.setSentimentLabel(s.label());
        a.setActivityDate(Instant.now());
        activities.save(a);

        boolean negative = "Negative".equals(s.label());
        lead.setFlagged(negative);                                                  // 4 (a later positive note clears it)
        ScoreResult score = ai.rescore(lead);                                       // 5 (also saves the lead)
        if (negative) alertOwner(lead);                                             // 6

        ActivityDto dto = new ActivityDto(a.getActivityId(), lead.getLeadId(), lead.getFullName(), me.userId(),
                me.fullName(), type.name(), text, a.getSentimentLabel(), a.getActivityDate());
        return new ActivityResult(dto, s, negative, score);
    }

    private void alertOwner(Lead lead) {
        var rules = settings.rules();
        if (!rules.isEmailOnNegative() || lead.getAssignedTo() == null) return;
        User owner = users.findById(lead.getAssignedTo()).orElse(null);
        if (owner == null) return;
        String body = "The latest interaction with " + lead.getFullName() + " (" + lead.getCompany()
                + ") was classified as negative. Please review the lead and follow up.";
        hub.send(owner.getUserId(), new NotificationDto("neg-" + System.nanoTime(), "Negative sentiment detected",
                lead.getFullName() + " needs attention", lead.getLeadId(), Instant.now()));
        email.send(owner.getEmail(), "Clientra: negative sentiment for " + lead.getFullName(), body);
    }

    private List<ActivityDto> toDtos(List<Activity> rows) {
        Set<Integer> leadIds = new HashSet<>(), userIds = new HashSet<>();
        for (Activity a : rows) { leadIds.add(a.getLeadId()); userIds.add(a.getUserId()); }
        Map<Integer, String> leadNames = new HashMap<>(), userNames = new HashMap<>();
        for (Lead l : leads.findAllById(leadIds)) leadNames.put(l.getLeadId(), l.getFullName());
        for (User u : users.findAllById(userIds)) userNames.put(u.getUserId(), u.getFullName());
        List<ActivityDto> out = new ArrayList<>();
        for (Activity a : rows) {
            out.add(new ActivityDto(a.getActivityId(), a.getLeadId(), leadNames.getOrDefault(a.getLeadId(), ""),
                    a.getUserId(), userNames.getOrDefault(a.getUserId(), ""), a.getType().name(), a.getDescription(),
                    a.getSentimentLabel(), a.getActivityDate()));
        }
        return out;
    }
}
