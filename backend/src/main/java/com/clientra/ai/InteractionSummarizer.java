package com.clientra.ai;

import com.clientra.ai.AiModels.SummaryResult;
import com.clientra.entity.Activity;
import com.clientra.entity.Lead;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Structured interaction summary (synopsis 3.8): counts by sentiment, the last three notes,
 * the sentiment trend and a suggested next action. Also supplies the trend and the "reliable"
 * flag when an LLM writes the prose (the synopsis says AI needs 3-5 touchpoints to be reliable).
 */
@Component
public class InteractionSummarizer {
    private static final Map<String, String> NEXT = Map.of(
            "New", "Make first contact", "Contacted", "Book a demo", "Qualified", "Send proposal",
            "Proposal", "Follow up on proposal feedback", "Negotiation", "Agree final terms and close");

    public SummaryResult summarize(Lead lead, List<Activity> activities, String stageName) {
        List<Activity> sorted = new ArrayList<>(activities);
        sorted.sort(Comparator.comparing(Activity::getActivityDate));    // oldest -> newest
        int total = sorted.size();

        List<String> keyPoints = new ArrayList<>();                      // last three notes
        for (int i = Math.max(0, total - 3); i < total; i++) keyPoints.add(sorted.get(i).getDescription());

        String trend = trend(sorted);
        List<String> actions = new ArrayList<>();
        actions.add(NEXT.getOrDefault(stageName, "Review lead status"));
        if ("Declining".equals(trend)) actions.add("Resolve the customer's concerns before pushing for a close");

        String text = total == 0
                ? lead.getFullName() + " has no interactions yet. Log a first touchpoint to enable AI insights."
                : lead.getFullName() + " (" + lead.getCompany() + ") is in the \"" + stageName + "\" stage with " + total
                + " logged interactions: " + count(sorted, "Positive") + " positive, " + count(sorted, "Neutral")
                + " neutral and " + count(sorted, "Negative") + " negative. Sentiment trend is " + trend.toLowerCase() + ".";
        return new SummaryResult(text, keyPoints, actions, trend, total >= 3);
    }

    /** Declining if either of the last two notes is negative, Improving if both are positive, else Stable. */
    public String trend(List<Activity> oldestFirst) {
        int n = oldestFirst.size();
        if (n == 0) return "Stable";
        List<Activity> lastTwo = oldestFirst.subList(Math.max(0, n - 2), n);
        boolean anyNeg = false, allPos = true;
        for (Activity a : lastTwo) {
            if ("Negative".equals(a.getSentimentLabel())) anyNeg = true;
            if (!"Positive".equals(a.getSentimentLabel())) allPos = false;
        }
        return anyNeg ? "Declining" : allPos ? "Improving" : "Stable";
    }

    private long count(List<Activity> list, String label) {
        return list.stream().filter(a -> label.equals(a.getSentimentLabel())).count();
    }
}
