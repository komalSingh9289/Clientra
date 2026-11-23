package com.clientra.service;

import com.clientra.dto.Dtos.LeadDto;
import com.clientra.dto.Dtos.NameValue;
import com.clientra.dto.Dtos.ReportDto;
import com.clientra.entity.PipelineStage;
import com.clientra.repository.StageRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Module 7 reports: filter by date range, stage, source and representative, then aggregate. */
@Service
public class ReportService {
    private final LeadService leadService;
    private final StageRepository stages;

    public ReportService(LeadService leadService, StageRepository stages) {
        this.leadService = leadService;
        this.stages = stages;
    }

    @Transactional(readOnly = true)
    public ReportDto build(LeadService.Filter f) {
        List<LeadDto> rows = leadService.list(f);             // same filtering + data scope as the lead list
        Map<String, Long> bySource = new LinkedHashMap<>();
        Map<String, Long> byStage = new LinkedHashMap<>();
        for (PipelineStage s : stages.findAllByOrderBySequenceOrderAsc()) byStage.put(s.getStageName(), 0L);
        double total = 0;
        for (LeadDto l : rows) {
            String src = l.source() == null || l.source().isBlank() ? "Unknown" : l.source();
            bySource.merge(src, 1L, Long::sum);
            byStage.merge(l.stageName(), 1L, Long::sum);
            total += l.openValue();
        }
        return new ReportDto(rows, toList(bySource), toList(byStage), total);
    }

    private List<NameValue> toList(Map<String, Long> m) {
        List<NameValue> out = new ArrayList<>();
        for (Map.Entry<String, Long> e : m.entrySet()) out.add(new NameValue(e.getKey(), e.getValue()));
        return out;
    }
}
