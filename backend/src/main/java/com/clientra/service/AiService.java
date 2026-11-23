package com.clientra.service;

import com.clientra.ai.*;
import com.clientra.ai.AiModels.*;
import com.clientra.dto.Dtos.AiLogDto;
import com.clientra.entity.*;
import com.clientra.repository.*;
import com.clientra.security.AuthUser;
import com.clientra.security.CurrentUser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * FACADE for Module 5 (AI-Powered Engagement Intelligence).
 * Controllers/services call this one class; it decides which engine answers:
 *   - sentiment, scoring, follow-up timing: always local (fast, free, deterministic)
 *   - message drafting and summaries: the LLM when configured (OpenAI / Gemini), otherwise the
 *     built-in rule-based engine. If the LLM errors out we fall back automatically.
 * Every explicit AI request is stored in tbl_ai_logs + tbl_ai_responses (the AI audit trail).
 */
@Service
public class AiService {
    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final LeadAccess access;
    private final ActivityRepository activities;
    private final DealRepository deals;
    private final LeadRepository leads;
    private final StageRepository stages;
    private final AiLogRepository aiLogs;
    private final AiResponseRepository aiResponses;
    private final SentimentAnalyzer sentiment;
    private final LeadScorer scorer;
    private final MessageGenerator messages;
    private final InteractionSummarizer summarizer;
    private final FollowUpPredictor predictor;
    private final LlmClient llm;
    private final CurrentUser current;
    private final ObjectMapper mapper;

    public AiService(LeadAccess access, ActivityRepository activities, DealRepository deals, LeadRepository leads,
                     StageRepository stages, AiLogRepository aiLogs, AiResponseRepository aiResponses,
                     SentimentAnalyzer sentiment, LeadScorer scorer, MessageGenerator messages,
                     InteractionSummarizer summarizer, FollowUpPredictor predictor, LlmClient llm,
                     CurrentUser current, ObjectMapper mapper) {
        this.access = access; this.activities = activities; this.deals = deals; this.leads = leads;
        this.stages = stages; this.aiLogs = aiLogs; this.aiResponses = aiResponses; this.sentiment = sentiment;
        this.scorer = scorer; this.messages = messages; this.summarizer = summarizer; this.predictor = predictor;
        this.llm = llm; this.current = current; this.mapper = mapper;
    }

    // ------------------------------------------------------------------ scoring

    /**
     * Recalculates a lead's score from its activities + deals and SAVES it on the lead.
     * Called automatically after every new activity / deal change / lead creation.
     */
    @Transactional
    public ScoreResult rescore(Lead lead) {
        List<Activity> acts = activities.findByLeadIdOrderByActivityDateDesc(lead.getLeadId());
        List<Deal> ds = deals.findByLeadId(lead.getLeadId());
        ScoreResult r = scorer.score(acts, ds, Instant.now());
        lead.setAiScore(r.score());
        lead.setPriorityLabel(r.priority());
        leads.save(lead);
        return r;
    }

    /** POST /ai/lead-score : explicit re-score requested by a user, logged. */
    @Transactional
    public ScoreResult scoreLead(Integer leadId) {
        Lead lead = access.require(leadId);
        ScoreResult r = rescore(lead);
        record(leadId, "LEAD_SCORE", "Lead #" + leadId, "score=" + r.score() + ", priority=" + r.priority(),
                (double) r.score(), null, null);
        return r;
    }

    // ---------------------------------------------------------------- sentiment

    /** POST /ai/sentiment : free-text tester, not tied to a lead. */
    @Transactional
    public SentimentResult analyzeText(String text) {
        SentimentResult r = sentiment.analyze(text);
        record(null, "SENTIMENT", text, r.label() + " (" + r.confidence() + ")", r.confidence(), null, null);
        return r;
    }

    /** Used by ActivityService each time an interaction note is saved. */
    @Transactional
    public SentimentResult analyzeForActivity(Integer leadId, String text) {
        SentimentResult r = sentiment.analyze(text);
        record(leadId, "SENTIMENT", text, r.label() + " (" + r.confidence() + ")", r.confidence(), null, null);
        return r;
    }

    // ------------------------------------------------------------ message draft

    /** POST /ai/draft-message. */
    @Transactional
    public DraftResult draft(Integer leadId, String tone) {
        Lead lead = access.require(leadId);
        List<Activity> acts = activities.findByLeadIdOrderByActivityDateDesc(leadId);
        String stageName = stageName(lead);
        AuthUser me = current.get();
        String sender = me.fullName();

        DraftResult result = messages.generate(lead, stageName, acts, tone, sender);   // rule-based baseline
        String engine = "rule-based";
        if (llm.enabled()) {
            String system = "You are a sales assistant inside a small-business CRM. Write a short, polite follow-up email. "
                    + "Reply with ONLY a JSON object: {\"subject\": \"...\", \"body\": \"...\"}. No markdown.";
            String user = "Lead: " + lead.getFullName() + " at " + lead.getCompany() + "\nPipeline stage: " + stageName
                    + "\nDesired tone: " + (tone == null ? "Friendly" : tone) + "\nSender name: " + sender
                    + "\nRecent interactions (newest first):\n" + describeActivities(acts, 5);
            Optional<String> out = llm.complete(system, user);
            if (out.isPresent()) {
                try {
                    JsonNode n = mapper.readTree(extractJson(out.get()));
                    String body = n.path("body").asText("");
                    String subject = n.path("subject").asText("");
                    if (!body.isBlank()) {
                        result = new DraftResult(subject.isBlank() ? result.subject() : subject, body);
                        engine = llm.engineName();
                    }
                } catch (Exception e) {
                    log.warn("Could not parse LLM draft, using rule-based draft: {}", e.getMessage());
                }
            }
        }
        record(leadId, "MESSAGE_DRAFT", "[" + engine + "] tone=" + tone + ", stage=" + stageName,
                result.subject() + "\n\n" + result.body(), null, result.subject(), null);
        return result;
    }

    // ------------------------------------------------------------------ summary

    /** POST /ai/summary. */
    @Transactional
    public SummaryResult summary(Integer leadId) {
        Lead lead = access.require(leadId);
        List<Activity> acts = activities.findByLeadIdOrderByActivityDateDesc(leadId);
        String stageName = stageName(lead);
        SummaryResult result = summarizer.summarize(lead, acts, stageName);     // rule-based baseline
        String engine = "rule-based";
        if (llm.enabled() && !acts.isEmpty()) {
            String system = "You summarise a sales lead's interaction history for a CRM hand-off. Reply with ONLY a JSON object: "
                    + "{\"summary\": \"2-3 sentences\", \"keyPoints\": [\"...\"], \"actionItems\": [\"...\"]}. No markdown.";
            String user = "Lead: " + lead.getFullName() + " at " + lead.getCompany() + "\nStage: " + stageName
                    + "\nInteractions (newest first):\n" + describeActivities(acts, 15);
            Optional<String> out = llm.complete(system, user);
            if (out.isPresent()) {
                try {
                    JsonNode n = mapper.readTree(extractJson(out.get()));
                    String text = n.path("summary").asText("");
                    List<String> points = strings(n.path("keyPoints"));
                    List<String> actions = strings(n.path("actionItems"));
                    if (!text.isBlank()) {
                        // trend + reliability stay locally computed, they are facts about the data
                        result = new SummaryResult(text, points.isEmpty() ? result.keyPoints() : points,
                                actions.isEmpty() ? result.actionItems() : actions, result.sentimentTrend(), result.reliable());
                        engine = llm.engineName();
                    }
                } catch (Exception e) {
                    log.warn("Could not parse LLM summary, using rule-based summary: {}", e.getMessage());
                }
            }
        }
        record(leadId, "SUMMARY", "[" + engine + "] " + acts.size() + " interactions", result.summary(), null,
                result.summary(), String.join("; ", result.actionItems()));
        return result;
    }

    // ---------------------------------------------------------------- follow-up

    /** GET /ai/follow-up-time/{leadId} (cheap and deterministic, so not logged). */
    @Transactional(readOnly = true)
    public FollowUpResult followUp(Integer leadId) {
        access.require(leadId);
        return predictor.best(activities.findByLeadIdOrderByActivityDateDesc(leadId));
    }

    // --------------------------------------------------------------------- logs

    /** GET /ai/logs : reps only see logs of their own leads. */
    @Transactional(readOnly = true)
    public List<AiLogDto> logs() {
        AuthUser me = current.get();
        List<AiLog> rows = me.isRep()
                ? aiLogs.findForOwner(me.userId(), PageRequest.of(0, 100))
                : aiLogs.findAllByOrderByTimestampDesc(PageRequest.of(0, 100));
        List<AiLogDto> out = new ArrayList<>();
        for (AiLog g : rows) {
            out.add(new AiLogDto(g.getLogId(), g.getLeadId(), g.getRequestType(), g.getInputSummary(), g.getAiResponse(), g.getTimestamp()));
        }
        return out;
    }

    // ------------------------------------------------------------------ helpers

    private void record(Integer leadId, String type, String input, String output, Double score, String summary, String recs) {
        AiLog g = new AiLog();
        g.setLeadId(leadId);
        g.setRequestType(type);
        g.setInputSummary(trunc(input, 2000));
        g.setAiResponse(trunc(output, 8000));
        g.setTimestamp(Instant.now());
        aiLogs.save(g);                                   // save() fills in the generated logId
        AiResponse r = new AiResponse();
        r.setLogId(g.getLogId());
        r.setInsightsScore(score);
        r.setSummary(trunc(summary, 8000));
        r.setRecommendations(trunc(recs, 8000));
        r.setCreatedAt(Instant.now());
        aiResponses.save(r);
    }

    private String stageName(Lead lead) {
        return stages.findById(lead.getStageId()).map(PipelineStage::getStageName).orElse("");
    }

    private String describeActivities(List<Activity> acts, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(max, acts.size()); i++) {
            Activity a = acts.get(i);
            sb.append("- ").append(a.getType()).append(" (").append(a.getSentimentLabel()).append("): ")
              .append(trunc(a.getDescription(), 400)).append('\n');
        }
        return sb.length() == 0 ? "(none yet)" : sb.toString();
    }

    /** LLMs sometimes wrap JSON in ```json fences or add a sentence; keep only the {...} part. */
    private String extractJson(String s) {
        int a = s.indexOf('{'), b = s.lastIndexOf('}');
        return (a >= 0 && b > a) ? s.substring(a, b + 1) : s;
    }

    private List<String> strings(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr != null && arr.isArray()) for (JsonNode n : arr) if (!n.asText("").isBlank()) out.add(n.asText());
        return out;
    }

    private String trunc(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
