package com.clientra.ai;

import com.clientra.ai.AiModels.SentimentResult;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Sentiment classifier (synopsis 3.7 / Module 5c).
 * A lexicon-based classifier: counts positive and negative cue words/phrases in the text.
 *   more negative cues -> Negative, more positive -> Positive, equal -> Neutral.
 * A positive word that is negated ("not interested", "never happy") counts as negative.
 * It runs locally, instantly and offline, so every saved interaction can be analysed for free.
 */
@Component
public class SentimentAnalyzer {
    private static final List<String> NEG = List.of("frustrat", "unhappy", "not satisfied", "expensive", "too costly",
            "cancel", "competitor", "other vendors", "delay", "disappoint", "angry", "complain", "issue", "problem",
            "not interested");
    private static final List<String> POS = List.of("happy", "loved", "love", "excited", "great", "interested",
            "impressed", "thank", "keen", "agreed", "ready to", "positive", "wants a quote", "approved");

    public SentimentResult analyze(String text) {
        String t = text == null ? "" : text.toLowerCase(Locale.ROOT);
        int neg = 0, pos = 0;
        for (String w : NEG) if (t.contains(w)) neg++;
        for (String w : POS) {
            int idx = t.indexOf(w);
            if (idx < 0) continue;
            if (isNegated(t, idx)) neg++; else pos++;
        }
        String label = neg > pos ? "Negative" : pos > neg ? "Positive" : "Neutral";
        double confidence = Math.min(0.55 + 0.15 * Math.abs(pos - neg), 0.97);
        return new SentimentResult(label, Math.round(confidence * 100.0) / 100.0, pos, neg);
    }

    /** Looks at the ~12 characters before the cue word for a negation such as "not ", "n't ", "never ", "no ". */
    private boolean isNegated(String t, int idx) {
        String before = t.substring(Math.max(0, idx - 12), idx);
        return before.contains("not ") || before.contains("n't ") || before.contains("never ") || before.endsWith("no ");
    }
}
