package com.clientra.web;

import com.clientra.dto.Dtos.ReorderRequest;
import com.clientra.dto.Dtos.StageNameRequest;
import com.clientra.entity.PipelineStage;
import com.clientra.service.StageService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Pipeline stages. Everyone can read them (the kanban needs them); only managers/admins can change them (UC-12). */
@RestController
@RequestMapping("/api/pipeline/stages")
public class StageController {
    private static final String MANAGERS = "hasAnyRole('ADMINISTRATOR','SALES_MANAGER')";
    private final StageService stages;

    public StageController(StageService stages) { this.stages = stages; }

    @GetMapping
    public List<PipelineStage> list() { return stages.list(); }

    @PostMapping
    @PreAuthorize(MANAGERS)
    public ResponseEntity<PipelineStage> create(@Valid @RequestBody StageNameRequest r) {
        return ResponseEntity.status(201).body(stages.create(r.stageName()));
    }

    /** PUT /api/pipeline/stages/order {ids:[...]} : full new order. */
    @PutMapping("/order")
    @PreAuthorize(MANAGERS)
    public List<PipelineStage> reorder(@Valid @RequestBody ReorderRequest r) { return stages.reorder(r.ids()); }

    @PutMapping("/{id}")
    @PreAuthorize(MANAGERS)
    public PipelineStage rename(@PathVariable Integer id, @Valid @RequestBody StageNameRequest r) { return stages.rename(id, r.stageName()); }

    @DeleteMapping("/{id}")
    @PreAuthorize(MANAGERS)
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        stages.delete(id);
        return ResponseEntity.noContent().build();
    }
}
