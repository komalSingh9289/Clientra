package com.clientra;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.clientra.ai.SentimentAnalyzer;
import org.junit.jupiter.api.Test;

/** Unit test (synopsis 10.1): the sentiment classifier on typical CRM notes and edge cases. */
class SentimentAnalyzerTest {
    private final SentimentAnalyzer analyzer = new SentimentAnalyzer();

    @Test void positiveNote() {
        assertEquals("Positive", analyzer.analyze("Loved the demo and asked for a quote for the full team").label());
    }

    @Test void negativeNote() {
        assertEquals("Negative", analyzer.analyze("Frustrated by the delayed response, considering other vendors").label());
    }

    @Test void neutralNote() {
        assertEquals("Neutral", analyzer.analyze("Shared product brochure and pricing sheet").label());
    }

    @Test void negatedPositiveCountsAsNegative() {
        assertEquals("Negative", analyzer.analyze("They are not happy").label());
    }

    @Test void nullAndEmptyAreNeutral() {
        assertEquals("Neutral", analyzer.analyze(null).label());
        assertEquals("Neutral", analyzer.analyze("").label());
    }
}
