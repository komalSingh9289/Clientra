package com.clientra.config;

import com.clientra.ai.AiModels.ScoreResult;
import com.clientra.ai.LeadScorer;
import com.clientra.ai.SentimentAnalyzer;
import com.clientra.entity.*;
import com.clientra.repository.*;
import com.clientra.security.Roles;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import static com.clientra.util.TimeUtil.ZONE;

/**
 * Runs once at start-up and makes sure the database is usable:
 *   1. the 3 roles, the 7 default pipeline stages and the notification-rule row always exist
 *   2. on an EMPTY database (no users):
 *        clientra.seed-demo-data=true  -> a demo company, 5 users, 14 leads with history, deals, reminders
 *        clientra.seed-demo-data=false -> only the first administrator from clientra.bootstrap-admin.*
 * Existing data is never touched, so restarting is safe.
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);

    private final RoleRepository roles;
    private final StageRepository stages;
    private final CompanyRepository companies;
    private final UserRepository users;
    private final LeadRepository leads;
    private final ActivityRepository activities;
    private final DealRepository deals;
    private final ReminderRepository reminders;
    private final AuditRepository audits;
    private final NotificationRulesRepository rules;
    private final PasswordEncoder encoder;
    private final SentimentAnalyzer sentiment;
    private final LeadScorer scorer;
    private final boolean seedDemo;
    private final String adminName, adminEmail, adminPassword;

    public DatabaseInitializer(RoleRepository roles, StageRepository stages, CompanyRepository companies, UserRepository users,
                               LeadRepository leads, ActivityRepository activities, DealRepository deals,
                               ReminderRepository reminders, AuditRepository audits, NotificationRulesRepository rules,
                               PasswordEncoder encoder, SentimentAnalyzer sentiment, LeadScorer scorer,
                               @Value("${clientra.seed-demo-data:true}") boolean seedDemo,
                               @Value("${clientra.bootstrap-admin.name}") String adminName,
                               @Value("${clientra.bootstrap-admin.email}") String adminEmail,
                               @Value("${clientra.bootstrap-admin.password}") String adminPassword) {
        this.roles = roles; this.stages = stages; this.companies = companies; this.users = users; this.leads = leads;
        this.activities = activities; this.deals = deals; this.reminders = reminders; this.audits = audits; this.rules = rules;
        this.encoder = encoder; this.sentiment = sentiment; this.scorer = scorer; this.seedDemo = seedDemo;
        this.adminName = adminName; this.adminEmail = adminEmail; this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        List<Role> roleList = ensureRoles();
        List<PipelineStage> stageList = ensureStages();
        if (rules.count() == 0) rules.save(new NotificationRules());
        if (users.count() > 0) return;                     // already initialised

        if (seedDemo) {
            seedDemo(roleList, stageList);
            log.warn("Demo data loaded. Demo logins: admin@clientra.demo / admin123, manager@clientra.demo / manager123, "
                    + "rep@clientra.demo / rep123. DO NOT use demo data in production (set CLIENTRA_SEED_DEMO=false).");
        } else {
            Company c = company("My Company");
            addUser(c, roleList.get(0), adminName, adminEmail, adminPassword, true, 0);
            log.warn("Created first administrator {} - change this password after the first login.", adminEmail);
        }
    }

    // ------------------------------------------------------------ base data

    private List<Role> ensureRoles() {
        if (roles.count() == 0) {
            roles.save(role(Roles.ADMIN, "Full system control"));
            roles.save(role(Roles.MANAGER, "Team oversight, reports, pipeline config"));
            roles.save(role(Roles.REP, "Own leads and interactions"));
        }
        List<Role> out = new ArrayList<>();
        for (String n : List.of(Roles.ADMIN, Roles.MANAGER, Roles.REP)) out.add(roles.findByRoleName(n).orElseThrow());
        return out;
    }

    private List<PipelineStage> ensureStages() {
        if (stages.count() == 0) {
            String[] names = {"New", "Contacted", "Qualified", "Proposal", "Negotiation", "Won", "Lost"};
            for (int i = 0; i < names.length; i++) {
                PipelineStage s = new PipelineStage();
                s.setStageName(names[i]);
                s.setSequenceOrder(i + 1);
                s.setStageType(i == 5 ? StageType.WON : i == 6 ? StageType.LOST : StageType.OPEN);
                stages.save(s);
            }
        }
        return stages.findAllByOrderBySequenceOrderAsc();
    }

    private Role role(String name, String desc) {
        Role r = new Role();
        r.setRoleName(name);
        r.setDescription(desc);
        return r;
    }

    private Company company(String name) {
        Company c = new Company();
        c.setCompanyName(name);
        return companies.save(c);
    }

    private User addUser(Company c, Role role, String name, String email, String password, boolean active, int createdDaysAgo) {
        User u = new User();
        u.setCompanyId(c.getCompanyId());
        u.setRoleId(role.getRoleId());
        u.setFullName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));      // bcrypt, work factor 12
        u.setActive(active);
        u.setCreatedAt(ago(createdDaysAgo, 10));
        return users.save(u);
    }

    // ------------------------------------------------------------- demo data

    private static final String[][] SEED = {
            // name, company, source, stageIndex(1-7), ownerIndex(1-5), interaction pattern, days since last, deal amount
            {"Priya Sharma", "Lotus Textiles", "Website", "5", "3", "PPNP", "4", "420000"},
            {"Vikram Singh", "Singh & Sons Hardware", "Referral", "4", "3", "NPPP", "1", "260000"},
            {"Ananya Rao", "BrightPath Academy", "LinkedIn", "3", "4", "NNP", "3", "150000"},
            {"Mohit Agarwal", "Agarwal Logistics", "Cold Call", "2", "3", "NG", "9", "0"},
            {"Sneha Iyer", "Iyer Organics", "Trade Show", "1", "4", "", "0", "0"},
            {"Rahul Khanna", "Khanna Motors", "Email Campaign", "5", "3", "NPPGP", "6", "780000"},
            {"Divya Menon", "Menon Pharma", "Referral", "6", "3", "NPPP", "12", "900000"},
            {"Arjun Bhatia", "Bhatia Interiors", "Website", "2", "4", "NNG", "2", "95000"},
            {"Kavita Desai", "Desai Foods", "LinkedIn", "4", "3", "PNPP", "5", "310000"},
            {"Sameer Qureshi", "Qureshi Tech Labs", "Website", "1", "3", "N", "8", "0"},
            {"Pooja Malhotra", "Malhotra Events", "Trade Show", "3", "4", "NPN", "2", "120000"},
            {"Nikhil Chauhan", "Chauhan Auto Parts", "Cold Call", "7", "3", "NGGG", "30", "200000"},
            {"Ritu Banerjee", "Banerjee Tea Co.", "Referral", "3", "4", "PNP", "1", "175000"},
            {"Harsh Vora", "Vora Digital", "Email Campaign", "2", "3", "PN", "4", "60000"}};
    private static final String[][] POOL = {
            {"Loved the demo and asked for a quote for the full team", "Very happy with the proposal, keen to discuss onboarding timeline",
                    "Excited about the AI lead scoring, will brief the director", "Impressed by the dashboard and agreed to a trial"},
            {"Shared product brochure and pricing sheet", "Asked for time to review internally", "Requested a call back next week",
                    "Discussed current process and spreadsheets they use"},
            {"Frustrated by the delayed response, considering other vendors", "Concerned that pricing is too expensive for their budget",
                    "Not satisfied with the demo, unhappy about missing integrations"}};
    private static final ActivityType[] TYPES = {ActivityType.Call, ActivityType.Email, ActivityType.Meeting, ActivityType.Note};

    private void seedDemo(List<Role> roleList, List<PipelineStage> stageList) {
        Company c = new Company();
        c.setCompanyName("Acme Retail Solutions");
        c.setEmail("hello@acme.in");
        c.setPhone("+91 98100 00000");
        c.setWebsite("acme.in");
        c.setAddress("Noida, UP");
        c.setIndustry("Retail");
        companies.save(c);

        List<User> u = new ArrayList<>();
        u.add(addUser(c, roleList.get(0), "Aarav Mehta", "admin@clientra.demo", "admin123", true, 200));
        u.add(addUser(c, roleList.get(1), "Neha Kapoor", "manager@clientra.demo", "manager123", true, 180));
        u.add(addUser(c, roleList.get(2), "Rohan Verma", "rep@clientra.demo", "rep123", true, 150));
        u.add(addUser(c, roleList.get(2), "Isha Nair", "isha@clientra.demo", "rep123", true, 120));
        u.add(addUser(c, roleList.get(2), "Karan Joshi", "karan@clientra.demo", "rep123", false, 90));
        u.get(0).setLastLogin(ago(0, 9));
        u.get(1).setLastLogin(ago(1, 9));
        u.get(2).setLastLogin(ago(0, 9));
        u.get(3).setLastLogin(ago(2, 9));
        u.get(4).setLastLogin(ago(40, 9));
        users.saveAll(u);

        Instant now = Instant.now();
        List<Lead> leadList = new ArrayList<>();
        List<List<Activity>> actsOf = new ArrayList<>();
        List<List<Deal>> dealsOf = new ArrayList<>();
        for (int i = 0; i < SEED.length; i++) {
            String[] r = SEED[i];
            int stageIdx = Integer.parseInt(r[3]);
            Integer owner = u.get(Integer.parseInt(r[4]) - 1).getUserId();
            Lead l = new Lead();
            l.setFullName(r[0]);
            l.setCompany(r[1]);
            l.setSource(r[2]);
            l.setStageId(stageList.get(stageIdx - 1).getStageId());
            l.setAssignedTo(owner);
            l.setEmail(r[0].split(" ")[0].toLowerCase() + "@" + r[1].split(" ")[0].toLowerCase().replaceAll("[^a-z]", "") + ".in");
            l.setPhone("+91 98" + (10000000 + i * 1234567));
            l.setCreatedAt(ago(40 + i * 3, 10));
            l.setStageChangedAt(ago(2 + (i % 5) * 3, 10));
            leads.save(l);

            // interaction history, oldest first; pattern letters P/N/G = positive/neutral/negative note
            List<Activity> acts = new ArrayList<>();
            String pattern = r[5];
            int lastDays = Integer.parseInt(r[6]);
            for (int k = 0; k < pattern.length(); k++) {
                String[] pool = POOL["PNG".indexOf(pattern.charAt(k))];
                String text = pool[(i + k) % pool.length];
                Activity a = new Activity();
                a.setLeadId(l.getLeadId());
                a.setUserId(owner);
                a.setType(TYPES[(i + k) % 4]);
                a.setDescription(text);
                a.setSentimentLabel(sentiment.analyze(text).label());
                a.setActivityDate(ago(lastDays + (pattern.length() - 1 - k) * 4, 9 + ((i + k) % 8)));
                acts.add(a);
            }
            activities.saveAll(acts);

            List<Deal> ds = new ArrayList<>();
            int amount = Integer.parseInt(r[7]);
            if (amount > 0) {
                Deal d = new Deal();
                d.setLeadId(l.getLeadId());
                d.setAmount(BigDecimal.valueOf(amount));
                d.setStatus(stageIdx == 6 ? DealStatus.Won : stageIdx == 7 ? DealStatus.Lost : DealStatus.Open);
                d.setClosingDate(LocalDate.now(ZONE).plusDays(10 + i * 2));
                d.setNotes("Annual licence for team seats");
                if (stageIdx == 6) d.setClosedAt(ago(15, 12));
                if (stageIdx == 7) d.setClosedAt(ago(35, 12));
                ds.add(d);
            }
            leadList.add(l);
            actsOf.add(acts);
            dealsOf.add(ds);
        }

        // Historical closed deals so the conversion-rate trend chart has data for the past months (demo only).
        // {leadIndex, W/L, monthsAgo, amount}
        Object[][] history = {{0, "W", 5, 120000}, {1, "L", 5, 80000}, {2, "L", 5, 60000}, {3, "W", 4, 95000}, {4, "L", 4, 70000},
                {5, "W", 3, 150000}, {6, "L", 3, 50000}, {7, "L", 3, 40000}, {8, "W", 2, 180000}, {9, "L", 2, 65000},
                {10, "W", 1, 110000}, {11, "L", 1, 55000}, {12, "L", 1, 45000}};
        for (Object[] h : history) {
            int idx = (Integer) h[0];
            boolean won = "W".equals(h[1]);
            Deal d = new Deal();
            d.setLeadId(leadList.get(idx).getLeadId());
            d.setAmount(BigDecimal.valueOf((Integer) h[3]));
            d.setStatus(won ? DealStatus.Won : DealStatus.Lost);
            ZonedDateTime closed = LocalDate.now(ZONE).minusMonths((Integer) h[2]).withDayOfMonth(10).atTime(12, 0).atZone(ZONE);
            d.setClosedAt(closed.toInstant());
            d.setClosingDate(closed.toLocalDate());
            d.setNotes(won ? "Earlier order" : "Earlier proposal, not taken up");
            dealsOf.get(idx).add(d);
        }
        for (List<Deal> ds : dealsOf) deals.saveAll(ds);

        // Score every lead with the same AI model the app uses at runtime.
        for (int i = 0; i < leadList.size(); i++) {
            Lead l = leadList.get(i);
            ScoreResult s = scorer.score(actsOf.get(i), dealsOf.get(i), now);
            l.setAiScore(s.score());
            l.setPriorityLabel(s.priority());
            List<Activity> acts = actsOf.get(i);
            l.setFlagged(!acts.isEmpty() && "Negative".equals(acts.get(acts.size() - 1).getSentimentLabel()));
            leads.save(l);
        }

        // {lead no., owner no., days from now, message, status}
        Object[][] rem = {
                {1, 3, -1.0, "Send revised quote to Lotus Textiles", ReminderStatus.Pending},
                {2, 3, 1.0, "Schedule onboarding call", ReminderStatus.Pending},
                {4, 3, -3.0, "Call back to address pricing concerns", ReminderStatus.Pending},
                {6, 3, 2.0, "Share case study with Khanna Motors", ReminderStatus.Pending},
                {7, 3, 4.0, "Contract review meeting", ReminderStatus.Pending},
                {3, 4, 0.1, "Demo follow-up email", ReminderStatus.Pending},
                {9, 3, -5.0, "Send product comparison sheet", ReminderStatus.Completed},
                {8, 4, -2.0, "Re-engage after negative feedback", ReminderStatus.Pending}};
        for (Object[] x : rem) {
            double days = (Double) x[2];
            Reminder r = new Reminder();
            r.setLeadId(leadList.get((Integer) x[0] - 1).getLeadId());
            r.setUserId(u.get((Integer) x[1] - 1).getUserId());
            Instant base = now.plusMillis((long) (days * 86_400_000L));
            r.setReminderTime(base.atZone(ZONE).withHour(11).withMinute(15).withSecond(0).withNano(0).toInstant());
            r.setMessage((String) x[3]);
            r.setStatus((ReminderStatus) x[4]);
            r.setPriority(days < 0 ? "High" : "Medium");
            r.setDueNotified(x[4] == ReminderStatus.Completed);
            reminders.save(r);
        }

        audit(u.get(0), "LOGIN", "Session", "Signed in", ago(0, 9));
        audit(u.get(1), "EXPORT", "Leads", "Exported lead data (CSV)", ago(1, 15));
        audit(u.get(0), "ROLE_CHANGE", "User #5", "Karan Joshi deactivated", ago(3, 12));
        audit(u.get(2), "DELETE", "Lead #99", "Deleted duplicate lead", ago(6, 11));
    }

    private void audit(User by, String action, String entity, String detail, Instant at) {
        AuditLog a = new AuditLog();
        a.setUserId(by.getUserId());
        a.setUserName(by.getFullName());
        a.setAction(action);
        a.setEntity(entity);
        a.setDetail(detail);
        a.setTimestamp(at);
        audits.save(a);
    }

    /** Same helper as the mock: N days ago at hour:15 in the business time zone. */
    private Instant ago(int days, int hour) {
        return LocalDate.now(ZONE).minusDays(days).atTime(hour, 15).atZone(ZONE).toInstant();
    }
}
