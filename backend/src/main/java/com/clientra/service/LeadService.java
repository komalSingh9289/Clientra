package com.clientra.service;

import com.clientra.dto.Dtos.*;
import com.clientra.entity.*;
import com.clientra.exception.ApiException;
import com.clientra.repository.*;
import com.clientra.security.AuthUser;
import com.clientra.security.CurrentUser;
import com.clientra.util.CsvUtil;
import com.clientra.util.TimeUtil;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Module 2 (Lead and Customer Data): CRUD, search/filter, duplicate detection, CSV import,
 * data-scope (reps see only their own leads) and the conversion of entities to LeadDto.
 *
 * Typical flow of create():  validate -> duplicate check -> save -> AI score -> audit -> DTO.
 */
@Service
public class LeadService {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final int MAX_IMPORT_ROWS = 5000;

    /** Optional list filters (all null = everything the caller may see). */
    public record Filter(String search, Integer stageId, String priority, String source,
                         Integer assignedTo, LocalDate from, LocalDate to) {}

    private final LeadRepository leads;
    private final StageRepository stages;
    private final UserRepository users;
    private final ActivityRepository activities;
    private final DealRepository deals;
    private final ReminderRepository reminders;
    private final AiLogRepository aiLogs;
    private final AiResponseRepository aiResponses;
    private final AiService ai;
    private final LeadAccess access;
    private final CurrentUser current;
    private final AuditService audit;

    public LeadService(LeadRepository leads, StageRepository stages, UserRepository users, ActivityRepository activities,
                       DealRepository deals, ReminderRepository reminders, AiLogRepository aiLogs,
                       AiResponseRepository aiResponses, AiService ai, LeadAccess access, CurrentUser current,
                       AuditService audit) {
        this.leads = leads; this.stages = stages; this.users = users; this.activities = activities;
        this.deals = deals; this.reminders = reminders; this.aiLogs = aiLogs; this.aiResponses = aiResponses;
        this.ai = ai; this.access = access; this.current = current; this.audit = audit;
    }

    // ------------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public List<LeadDto> list(Filter f) {
        AuthUser me = current.get();
        Specification<Lead> spec = (root, q, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (me.isRep()) p.add(cb.equal(root.get("assignedTo"), me.userId()));     // data scope
            if (f != null) {
                if (f.search() != null && !f.search().isBlank()) {
                    String like = "%" + f.search().trim().toLowerCase(Locale.ROOT) + "%";
                    p.add(cb.or(cb.like(cb.lower(root.<String>get("fullName")), like),
                                cb.like(cb.lower(root.<String>get("company")), like),
                                cb.like(cb.lower(root.<String>get("email")), like),
                                cb.like(cb.lower(root.<String>get("phone")), like)));
                }
                if (f.stageId() != null) p.add(cb.equal(root.get("stageId"), f.stageId()));
                if (f.priority() != null && !f.priority().isBlank()) p.add(cb.equal(root.get("priorityLabel"), f.priority()));
                if (f.source() != null && !f.source().isBlank()) p.add(cb.equal(root.get("source"), f.source()));
                if (f.assignedTo() != null && !me.isRep()) p.add(cb.equal(root.get("assignedTo"), f.assignedTo()));
                if (f.from() != null) p.add(cb.greaterThanOrEqualTo(root.<Instant>get("createdAt"), TimeUtil.startOfDay(f.from())));
                if (f.to() != null) p.add(cb.lessThanOrEqualTo(root.<Instant>get("createdAt"), TimeUtil.endOfDay(f.to())));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        // best leads first (highest AI score), id as a stable tie-breaker
        List<Lead> rows = leads.findAll(spec, Sort.by(Sort.Direction.DESC, "aiScore").and(Sort.by("leadId")));
        return toDtos(rows);
    }

    @Transactional(readOnly = true)
    public LeadDto get(Integer id) {
        return toDto(access.require(id));
    }

    // ------------------------------------------------------------------ write

    @Transactional
    public LeadDto create(LeadRequest r, boolean force) {
        AuthUser me = current.get();
        String email = clean(r.email());
        String phone = clean(r.phone());
        if (!force && !findDuplicates(email, phone, null).isEmpty()) throw duplicate();

        Lead l = new Lead();
        l.setFullName(r.fullName().trim());
        l.setEmail(email);
        l.setPhone(phone);
        l.setCompany(clean(r.company()));
        l.setSource(clean(r.source()) == null ? "Other" : clean(r.source()));
        l.setStageId(r.stageId() != null ? requireStage(r.stageId()).getStageId() : firstStage().getStageId());
        l.setAssignedTo(resolveOwner(me, r.assignedTo()));
        l.setCreatedAt(Instant.now());
        l.setStageChangedAt(Instant.now());
        leads.save(l);
        ai.rescore(l);                                       // initial AI score / priority
        audit.log("CREATE", "Lead #" + l.getLeadId(), "Created lead " + l.getFullName());
        return toDto(l);
    }

    @Transactional
    public LeadDto update(Integer id, LeadRequest r) {
        AuthUser me = current.get();
        Lead l = access.require(id);
        String email = clean(r.email());
        String phone = clean(r.phone());
        if (!findDuplicates(email, phone, id).isEmpty()) {
            throw ApiException.conflict("Another lead already uses this email or phone.", "DUPLICATE");
        }
        l.setFullName(r.fullName().trim());
        l.setEmail(email);
        l.setPhone(phone);
        l.setCompany(clean(r.company()));
        if (clean(r.source()) != null) l.setSource(clean(r.source()));
        if (r.stageId() != null && !r.stageId().equals(l.getStageId())) {
            l.setStageId(requireStage(r.stageId()).getStageId());
            l.setStageChangedAt(Instant.now());
        }
        // Only managers/admins may reassign; reps keep ownership of their own lead.
        if (!me.isRep() && r.assignedTo() != null && !r.assignedTo().equals(l.getAssignedTo())) {
            l.setAssignedTo(resolveOwner(me, r.assignedTo()));
        }
        leads.save(l);
        audit.log("UPDATE", "Lead #" + id, "Updated lead " + l.getFullName());
        return toDto(l);
    }

    /** Kanban drag-and-drop (UC-03). */
    @Transactional
    public LeadDto updateStage(Integer id, Integer stageId) {
        Lead l = access.require(id);
        PipelineStage s = requireStage(stageId);
        if (!s.getStageId().equals(l.getStageId())) {
            l.setStageId(s.getStageId());
            l.setStageChangedAt(Instant.now());
            leads.save(l);
            audit.log("UPDATE", "Lead #" + id, "Moved " + l.getFullName() + " to stage " + s.getStageName());
        }
        return toDto(l);
    }

    /** Deletes the lead and everything that hangs off it (activities, deals, reminders, AI logs). */
    @Transactional
    public void delete(Integer id) {
        Lead l = access.require(id);
        activities.deleteByLeadId(id);
        deals.deleteByLeadId(id);
        reminders.deleteByLeadId(id);
        List<AiLog> logs = aiLogs.findByLeadId(id);
        if (!logs.isEmpty()) {
            List<Integer> ids = new ArrayList<>();
            for (AiLog g : logs) ids.add(g.getLogId());
            aiResponses.deleteByLogIds(ids);
            aiLogs.deleteAll(logs);
        }
        leads.delete(l);
        audit.log("DELETE", "Lead #" + id, "Deleted lead " + l.getFullName());
    }

    // ----------------------------------------------------------------- import

    /** CSV file upload (multipart) -> same pipeline as the JSON import below. */
    @Transactional
    public ImportResult importCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Please choose a CSV file.");
        try {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            return bulkImport(CsvUtil.parse(text));
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read the uploaded file.");
        }
    }

    /**
     * Imports rows (keys: name|fullname, email, phone, company, source). Each row is validated
     * on its own: bad rows are skipped with a reason, good rows are saved (Section 3.2 CSV bulk import).
     * Row numbers are spreadsheet row numbers (row 1 = header).
     */
    @Transactional
    public ImportResult bulkImport(List<Map<String, String>> rows) {
        if (rows.size() > MAX_IMPORT_ROWS) throw ApiException.badRequest("Import is limited to " + MAX_IMPORT_ROWS + " rows at a time.");
        AuthUser me = current.get();
        Integer stageId = firstStage().getStageId();
        int imported = 0;
        List<SkippedRow> skipped = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            int rowNo = i + 2;
            String name = clean(first(row.get("name"), row.get("fullname")));
            String email = clean(row.get("email"));
            String phone = clean(row.get("phone"));
            if (name == null) { skipped.add(new SkippedRow(rowNo, "Missing name")); continue; }
            if (email != null && !EMAIL.matcher(email).matches()) { skipped.add(new SkippedRow(rowNo, "Invalid email")); continue; }
            if (!findDuplicates(email, phone, null).isEmpty()) { skipped.add(new SkippedRow(rowNo, "Duplicate of an existing lead")); continue; }
            Lead l = new Lead();
            l.setFullName(trimTo(name, 120));
            l.setEmail(email);
            l.setPhone(trimTo(phone, 30));
            l.setCompany(trimTo(clean(row.get("company")), 120));
            l.setSource(clean(row.get("source")) == null ? "Import" : trimTo(clean(row.get("source")), 100));
            l.setStageId(stageId);
            l.setAssignedTo(me.userId());
            leads.save(l);
            ai.rescore(l);
            imported++;
        }
        audit.log("IMPORT", "Leads", "Imported " + imported + " lead(s), skipped " + skipped.size());
        return new ImportResult(imported, skipped);
    }

    // ------------------------------------------------------------- duplicates

    /** Duplicate rule (synopsis 3.2): same e-mail (case-insensitive) OR same phone (ignoring spaces). */
    public List<Lead> findDuplicates(String email, String phone, Integer excludeId) {
        Map<Integer, Lead> found = new LinkedHashMap<>();
        if (email != null) for (Lead x : leads.findByEmailIgnoreCase(email)) found.put(x.getLeadId(), x);
        String p = phone == null ? "" : phone.replaceAll("\\s+", "");
        if (!p.isEmpty()) for (Lead x : leads.findByNormalizedPhone(p)) found.put(x.getLeadId(), x);
        if (excludeId != null) found.remove(excludeId);
        return new ArrayList<>(found.values());
    }

    private ApiException duplicate() {
        return ApiException.conflict("A lead with this email or phone number already exists.", "DUPLICATE");
    }

    // ------------------------------------------------------------ DTO mapping

    public LeadDto toDto(Lead l) { return toDtos(List.of(l)).get(0); }

    /**
     * Converts leads to DTOs with a fixed number of queries (no N+1): stage names, owner names,
     * last-activity time per lead and open-deal value per lead are all loaded in bulk.
     */
    public List<LeadDto> toDtos(List<Lead> rows) {
        Instant now = Instant.now();
        Map<Integer, PipelineStage> stageMap = new HashMap<>();
        for (PipelineStage s : stages.findAll()) stageMap.put(s.getStageId(), s);
        Map<Integer, String> owners = new HashMap<>();
        for (User u : users.findAll()) owners.put(u.getUserId(), u.getFullName());
        Map<Integer, Instant> lastActivity = new HashMap<>();
        for (Object[] r : activities.lastActivityByLead()) lastActivity.put((Integer) r[0], (Instant) r[1]);
        Map<Integer, Double> openValue = new HashMap<>();
        for (Object[] r : deals.sumByLeadForStatus(DealStatus.Open)) openValue.put((Integer) r[0], ((Number) r[1]).doubleValue());

        List<LeadDto> out = new ArrayList<>();
        for (Lead l : rows) {
            PipelineStage s = stageMap.get(l.getStageId());
            Instant last = lastActivity.get(l.getLeadId());
            long idle = TimeUtil.daysBetween(last != null ? last : l.getCreatedAt(), now);
            out.add(new LeadDto(l.getLeadId(), l.getFullName(), l.getEmail(), l.getPhone(), l.getCompany(), l.getSource(),
                    l.getStageId(), s == null ? "" : s.getStageName(), l.getAssignedTo(),
                    owners.getOrDefault(l.getAssignedTo(), ""), l.getPriorityLabel(), l.getAiScore(), l.isFlagged(),
                    l.getCreatedAt(), l.getStageChangedAt(), last, idle,
                    TimeUtil.daysBetween(l.getStageChangedAt(), now), openValue.getOrDefault(l.getLeadId(), 0.0)));
        }
        return out;
    }

    // ---------------------------------------------------------------- helpers

    private PipelineStage requireStage(Integer id) {
        return stages.findById(id).orElseThrow(() -> ApiException.badRequest("That pipeline stage does not exist."));
    }

    private PipelineStage firstStage() {
        List<PipelineStage> all = stages.findAllByOrderBySequenceOrderAsc();
        if (all.isEmpty()) throw ApiException.badRequest("No pipeline stages are configured.");
        return all.get(0);
    }

    /** Reps always own what they create; managers/admins may assign to any ACTIVE user (default: themselves). */
    private Integer resolveOwner(AuthUser me, Integer requested) {
        if (me.isRep() || requested == null) return me.userId();
        User u = users.findById(requested).orElseThrow(() -> ApiException.badRequest("The selected owner does not exist."));
        if (!u.isActive()) throw ApiException.badRequest("The selected owner is deactivated.");
        return u.getUserId();
    }

    private static String clean(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String first(String a, String b) { return (a != null && !a.isBlank()) ? a : b; }

    private static String trimTo(String s, int max) { return s == null ? null : (s.length() <= max ? s : s.substring(0, max)); }
}
