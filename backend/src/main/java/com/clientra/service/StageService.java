package com.clientra.service;

import com.clientra.entity.PipelineStage;
import com.clientra.entity.StageType;
import com.clientra.exception.ApiException;
import com.clientra.repository.LeadRepository;
import com.clientra.repository.StageRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Module 3 / UC-12: configurable pipeline stages (add, rename, reorder, delete). */
@Service
public class StageService {
    private final StageRepository stages;
    private final LeadRepository leads;
    private final AuditService audit;

    public StageService(StageRepository stages, LeadRepository leads, AuditService audit) {
        this.stages = stages;
        this.leads = leads;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<PipelineStage> list() { return stages.findAllByOrderBySequenceOrderAsc(); }

    @Transactional
    public PipelineStage create(String name) {
        String n = unique(name, null);
        int next = stages.findAll().stream().mapToInt(PipelineStage::getSequenceOrder).max().orElse(0) + 1;
        PipelineStage s = new PipelineStage();
        s.setStageName(n);
        s.setSequenceOrder(next);
        s.setStageType(StageType.OPEN);        // custom stages are always "in progress"
        stages.save(s);
        audit.log("CONFIG", "Stage #" + s.getStageId(), "Created pipeline stage " + n);
        return s;
    }

    @Transactional
    public PipelineStage rename(Integer id, String name) {
        PipelineStage s = stages.findById(id).orElseThrow(() -> ApiException.notFound("Stage not found."));
        String old = s.getStageName();
        s.setStageName(unique(name, id));
        stages.save(s);
        audit.log("CONFIG", "Stage #" + id, "Renamed stage " + old + " to " + s.getStageName());
        return s;
    }

    /** Receives ALL stage ids in the new order. */
    @Transactional
    public List<PipelineStage> reorder(List<Integer> ids) {
        List<PipelineStage> all = stages.findAll();
        Set<Integer> existing = new HashSet<>();
        for (PipelineStage s : all) existing.add(s.getStageId());
        if (ids.size() != existing.size() || !existing.equals(new HashSet<>(ids))) {
            throw ApiException.badRequest("The new order must list every stage exactly once.");
        }
        for (PipelineStage s : all) s.setSequenceOrder(ids.indexOf(s.getStageId()) + 1);
        stages.saveAll(all);
        audit.log("CONFIG", "Pipeline", "Reordered pipeline stages");
        return stages.findAllByOrderBySequenceOrderAsc();
    }

    @Transactional
    public void delete(Integer id) {
        PipelineStage s = stages.findById(id).orElseThrow(() -> ApiException.notFound("Stage not found."));
        if (s.getStageType() != StageType.OPEN) {
            throw ApiException.badRequest("The Won and Lost stages are needed for reporting and cannot be deleted. You can rename them.");
        }
        if (leads.existsByStageId(id)) {
            throw ApiException.badRequest("Move the leads out of this stage before deleting it.");
        }
        stages.delete(s);
        audit.log("CONFIG", "Stage #" + id, "Deleted pipeline stage " + s.getStageName());
    }

    /** Trims the name and rejects blanks and (case-insensitive) duplicates. */
    private String unique(String name, Integer selfId) {
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) throw ApiException.badRequest("Stage name is required.");
        for (PipelineStage s : stages.findAll()) {
            if (s.getStageName().equalsIgnoreCase(n) && !s.getStageId().equals(selfId)) {
                throw ApiException.conflict("A stage with that name already exists.", "DUPLICATE");
            }
        }
        return n;
    }
}
