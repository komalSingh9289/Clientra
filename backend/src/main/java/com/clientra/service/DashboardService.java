package com.clientra.service;

import com.clientra.ai.InsightsGenerator;
import com.clientra.dto.Dtos.*;
import com.clientra.entity.*;
import com.clientra.repository.*;
import com.clientra.security.AuthUser;
import com.clientra.security.CurrentUser;
import com.clientra.security.Roles;
import com.clientra.util.TimeUtil;
import java.time.Instant;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Module 7 (Analytics): everything the dashboard shows, in ONE call (GET /dashboard).
 * Data scope: a rep's numbers cover only their own leads; managers/admins see the whole team.
 * Definitions:
 *   active lead       = lead whose stage is of type OPEN
 *   pipeline value    = sum of OPEN deals
 *   conversion rate   = won / (won + lost) deals, in percent
 *   conversion trend  = same rate per calendar month, using the date each deal was closed
 */
@Service
public class DashboardService {
    private final LeadRepository leads;
    private final DealRepository deals;
    private final ReminderRepository reminders;
    private final ActivityRepository activities;
    private final StageRepository stages;
    private final UserRepository users;
    private final RoleRepository roles;
    private final LeadService leadService;
    private final ReminderService reminderService;
    private final InsightsGenerator insights;
    private final CurrentUser current;

    public DashboardService(LeadRepository leads, DealRepository deals, ReminderRepository reminders, ActivityRepository activities,
                            StageRepository stages, UserRepository users, RoleRepository roles, LeadService leadService,
                            ReminderService reminderService, InsightsGenerator insights, CurrentUser current) {
        this.leads = leads; this.deals = deals; this.reminders = reminders; this.activities = activities;
        this.stages = stages; this.users = users; this.roles = roles; this.leadService = leadService;
        this.reminderService = reminderService; this.insights = insights; this.current = current;
    }

    @Transactional(readOnly = true)
    public DashboardDto build() {
        AuthUser me = current.get();
        boolean rep = me.isRep();
        Instant now = Instant.now();

        List<Lead> myLeads = rep ? leads.findByAssignedTo(me.userId()) : leads.findAll();
        List<LeadDto> dtos = leadService.toDtos(myLeads);
        List<Deal> myDeals = rep ? deals.findForOwner(me.userId()) : deals.findAll();
        List<PipelineStage> stageList = stages.findAllByOrderBySequenceOrderAsc();
        Map<Integer, StageType> stageType = new HashMap<>();
        for (PipelineStage s : stageList) stageType.put(s.getStageId(), s.getStageType());

        List<LeadDto> active = dtos.stream().filter(l -> stageType.get(l.stageId()) == StageType.OPEN).collect(Collectors.toList());

        // ---- KPIs
        double pipelineValue = 0;
        long won = 0, lost = 0;
        for (Deal d : myDeals) {
            if (d.getStatus() == DealStatus.Open) pipelineValue += d.getAmount().doubleValue();
            if (d.getStatus() == DealStatus.Won) won++;
            if (d.getStatus() == DealStatus.Lost) lost++;
        }
        double conversion = (won + lost) == 0 ? 0 : round1(won * 100.0 / (won + lost));
        Instant weekAgo = now.minus(7, ChronoUnit.DAYS);
        long newThisWeek = dtos.stream().filter(l -> l.createdAt().isAfter(weekAgo)).count();
        Kpis kpis = new Kpis(active.size(), newThisWeek, pipelineValue, conversion);

        // ---- open value per OPEN stage (bar chart)
        List<StageValue> valueByStage = new ArrayList<>();
        for (PipelineStage s : stageList) {
            if (s.getStageType() != StageType.OPEN) continue;
            double v = 0;
            long c = 0;
            for (LeadDto l : dtos) if (l.stageId().equals(s.getStageId())) { v += l.openValue(); c++; }
            valueByStage.add(new StageValue(s.getStageName(), v, c));
        }

        // ---- conversion trend, last 6 months
        List<TrendPoint> trend = new ArrayList<>();
        YearMonth thisMonth = YearMonth.now(TimeUtil.ZONE);
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = thisMonth.minusMonths(i);
            long w = 0, l = 0;
            for (Deal d : myDeals) {
                if (d.getClosedAt() == null || !YearMonth.from(d.getClosedAt().atZone(TimeUtil.ZONE)).equals(ym)) continue;
                if (d.getStatus() == DealStatus.Won) w++;
                if (d.getStatus() == DealStatus.Lost) l++;
            }
            trend.add(new TrendPoint(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH), (w + l) == 0 ? 0 : round1(w * 100.0 / (w + l))));
        }

        // ---- top leads + overdue follow-ups
        List<LeadDto> top = active.stream().sorted(Comparator.comparingInt(LeadDto::aiScore).reversed()).limit(5).collect(Collectors.toList());
        List<Reminder> overdueRows = new ArrayList<>();
        for (Reminder r : reminders.findByStatusAndReminderTimeLessThanEqual(ReminderStatus.Pending, now)) {
            if (!rep || r.getUserId().equals(me.userId())) overdueRows.add(r);
        }
        overdueRows.sort(Comparator.comparing(Reminder::getReminderTime));
        List<ReminderDto> overdue = reminderService.toDtos(overdueRows.size() > 10 ? overdueRows.subList(0, 10) : overdueRows);

        // ---- team table (managers / admins only)
        List<RepPerformance> reps = new ArrayList<>();
        if (!rep) {
            Role repRole = roles.findByRoleName(Roles.REP).orElse(null);
            Map<Integer, Integer> ownerOfLead = new HashMap<>();
            for (Lead l : myLeads) ownerOfLead.put(l.getLeadId(), l.getAssignedTo());
            if (repRole != null) {
                for (User u : users.findByRoleIdIn(List.of(repRole.getRoleId()))) {
                    long leadCount = myLeads.stream().filter(l -> u.getUserId().equals(l.getAssignedTo())).count();
                    long wonCount = myDeals.stream().filter(d -> d.getStatus() == DealStatus.Won
                            && u.getUserId().equals(ownerOfLead.get(d.getLeadId()))).count();
                    reps.add(new RepPerformance(u.getFullName(), leadCount, activities.countByUserId(u.getUserId()), wonCount));
                }
            }
        }

        // ---- AI insights
        List<LeadDto> stalled = active.stream().filter(l -> "High".equals(l.priorityLabel()) && l.idleDays() > 7).limit(2).collect(Collectors.toList());
        long negative = active.stream().filter(LeadDto::flagged).count();
        return new DashboardDto(kpis, valueByStage, trend, top, overdue, reps, insights.generate(stalled, negative, trend));
    }

    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
