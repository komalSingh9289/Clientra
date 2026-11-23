package com.clientra.ai;

import java.util.List;

/** Result shapes returned by the AI endpoints. Field names are what the React app reads. */
public final class AiModels {
    private AiModels() {}

    public record SentimentResult(String label, double confidence, int positiveHits, int negativeHits) {}
    public record Factor(String label, int value, int max) {}
    public record ScoreResult(int score, String priority, List<Factor> factors) {}
    public record DraftResult(String subject, String body) {}
    public record SummaryResult(String summary, List<String> keyPoints, List<String> actionItems,
                                String sentimentTrend, boolean reliable) {}
    public record FollowUpResult(String day, String window, String reason) {}
}
