package com.clientra.ai;

import com.clientra.ai.AiModels.Factor;
import com.clientra.ai.AiModels.ScoreResult;
import com.clientra.entity.Activity;
import com.clientra.entity.Deal;
import com.clientra.entity.DealStatus;
import com.clientra.util.TimeUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Smart lead scoring (synopsis 3.5 / Module 5a).
 * A transparent weighted model over the four behavioural signals named in the synopsis:
 *   1. Interaction frequency  - more touchpoints = more engaged        (0-25)
 *   2. Engagement recency     - recent contact scores higher           (0-25)
 *   3. Customer sentiment     - share of positive vs negative notes    (0-25)
 *   4. Deal value             - bigger live opportunity scores higher  (0-25)
 * Total 0-100 -> priority High (>=65), Medium (>=40), Low.
 * The score is recalculated whenever an activity or deal changes.
 */
@Component
public class LeadScorer {

    public ScoreResult score(List<Activity> activities, List<Deal> deals, Instant now) {
        int n = activities.size();

        long lastDays = Long.MAX_VALUE;                         // days since the most recent touchpoint
        int pos = 0, neg = 0;
        for (Activity a : activities) {
            lastDays = Math.min(lastDays, TimeUtil.daysBetween(a.getActivityDate(), now));
            if ("Positive".equals(a.getSentimentLabel())) pos++;
            if ("Negative".equals(a.getSentimentLabel())) neg++;
        }

        double liveValue = 0;                                   // open + won deals count, lost deals do not
        for (Deal d : deals) if (d.getStatus() != DealStatus.Lost) liveValue += d.getAmount().doubleValue();

        int frequency = (int) Math.round(Math.min(n / 6.0, 1.0) * 25);
        int recency = n == 0 ? 0 : lastDays <= 2 ? 25 : lastDays <= 7 ? 18 : lastDays <= 14 ? 10 : 3;
        int sentiment = n == 0 ? 10 : (int) Math.round(12.5 + 12.5 * ((pos - neg) / (double) n));
        int value = (int) Math.round(Math.min(liveValue / 500000.0, 1.0) * 25);

        List<Factor> factors = new ArrayList<>();
        factors.add(new Factor("Interaction frequency", frequency, 25));
        factors.add(new Factor("Engagement recency", recency, 25));
        factors.add(new Factor("Customer sentiment", sentiment, 25));
        factors.add(new Factor("Deal value", value, 25));

        int total = frequency + recency + sentiment + value;
        String priority = total >= 65 ? "High" : total >= 40 ? "Medium" : "Low";
        return new ScoreResult(total, priority, factors);
    }
}
