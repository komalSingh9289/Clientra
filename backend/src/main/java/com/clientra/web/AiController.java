package com.clientra.web;

import com.clientra.ai.AiModels.*;
import com.clientra.dto.Dtos.*;
import com.clientra.service.AiService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** Module 5 endpoints: UC-05 AI draft, UC-06 AI summary, UC-07 lead score. */
@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService ai;

    public AiController(AiService ai) { this.ai = ai; }

    @PostMapping("/draft-message")
    public DraftResult draft(@Valid @RequestBody DraftRequest r) { return ai.draft(r.leadId(), r.tone()); }

    @PostMapping("/summary")
    public SummaryResult summary(@Valid @RequestBody LeadIdRequest r) { return ai.summary(r.leadId()); }

    @PostMapping("/lead-score")
    public ScoreResult score(@Valid @RequestBody LeadIdRequest r) { return ai.scoreLead(r.leadId()); }

    @PostMapping("/sentiment")
    public SentimentResult sentiment(@Valid @RequestBody TextRequest r) { return ai.analyzeText(r.text()); }

    @GetMapping("/follow-up-time/{leadId}")
    public FollowUpResult followUp(@PathVariable Integer leadId) { return ai.followUp(leadId); }

    @GetMapping("/logs")
    public List<AiLogDto> logs() { return ai.logs(); }
}
