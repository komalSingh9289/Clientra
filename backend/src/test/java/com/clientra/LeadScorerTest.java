package com.clientra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clientra.ai.AiModels.ScoreResult;
import com.clientra.ai.LeadScorer;
import com.clientra.entity.*;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit test: the four behavioural signals of the lead-scoring model. */
class LeadScorerTest {
    private final LeadScorer scorer = new LeadScorer();

    private Activity act(String sentiment, int daysAgo) {
        Activity a = new Activity();
        a.setType(ActivityType.Call);
        a.setSentimentLabel(sentiment);
        a.setActivityDate(Instant.now().minus(Duration.ofDays(daysAgo)));
        return a;
    }

    @Test void engagedLeadWithBigDealIsHighPriority() {
        Deal d = new Deal();
        d.setAmount(new BigDecimal("420000"));
        d.setStatus(DealStatus.Open);
        ScoreResult r = scorer.score(List.of(act("Neutral", 16), act("Positive", 12), act("Neutral", 8), act("Positive", 4)),
                List.of(d), Instant.now());
        assertEquals(17, r.factors().get(0).value());   // frequency: 4 of 6 touchpoints
        assertEquals(18, r.factors().get(1).value());   // recency: last contact within 7 days
        assertEquals(19, r.factors().get(2).value());   // sentiment: 2 positive of 4
        assertEquals(21, r.factors().get(3).value());   // value: 420k of 500k cap
        assertEquals(75, r.score());
        assertEquals("High", r.priority());
    }

    @Test void leadWithNoHistoryIsLowPriority() {
        ScoreResult r = scorer.score(List.of(), List.of(), Instant.now());
        assertEquals(10, r.score());
        assertEquals("Low", r.priority());
    }

    @Test void lostDealsDoNotCountAsValue() {
        Deal d = new Deal();
        d.setAmount(new BigDecimal("500000"));
        d.setStatus(DealStatus.Lost);
        assertEquals(0, scorer.score(List.of(), List.of(d), Instant.now()).factors().get(3).value());
    }
}
