package com.clientra.ai;

import com.clientra.ai.AiModels.DraftResult;
import com.clientra.entity.Activity;
import com.clientra.entity.Lead;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Template-based follow-up writer (synopsis 3.6). It is the always-available fallback behind the
 * LLM: it adapts the text to the lead's pipeline stage, the last interaction and its sentiment.
 */
@Component
public class MessageGenerator {
    private static final Map<String, String> BY_STAGE = Map.of(
            "New", "I would love to understand your current process and show how we can save your team time.",
            "Contacted", "I am following up to find a 20-minute slot for a quick walkthrough of the platform.",
            "Qualified", "Based on what you shared, I have put together a short proposal tailored to your team size.",
            "Proposal", "Do you have any questions on the proposal? I am happy to adjust scope or pricing where it makes sense.",
            "Negotiation", "I am keen to close the remaining points so your team can start onboarding this month.");

    public DraftResult generate(Lead lead, String stageName, List<Activity> activities, String tone, String sender) {
        String t = tone == null || tone.isBlank() ? "Friendly" : tone;
        String first = lead.getFullName().trim().split("\\s+")[0];
        String company = lead.getCompany() == null || lead.getCompany().isBlank() ? "your company" : lead.getCompany();

        Activity last = null;                                   // most recent interaction
        for (Activity a : activities) if (last == null || a.getActivityDate().isAfter(last.getActivityDate())) last = a;
        boolean upset = last != null && "Negative".equals(last.getSentimentLabel());

        String hello = "Professional".equals(t) ? "Dear " + lead.getFullName() + "," : "Hi " + first + ",";
        String opener;
        if (upset) opener = "Thank you for your honest feedback, and apologies for any inconvenience so far. We would like to put this right.";
        else if (last != null) opener = "Thanks for our recent " + last.getType().name().toLowerCase() + "; it was great learning more about " + company + ".";
        else opener = "Thanks for your interest in Clientra for " + company + ".";

        String mid = BY_STAGE.getOrDefault(stageName, "Please let me know how I can help with next steps.");
        String body = "Concise".equals(t)
                ? hello + "\n\n" + mid + "\n\nBest,\n" + sender
                : hello + "\n\n" + opener + " " + mid + "\n\nWould a short call this week work for you?\n\nWarm regards,\n" + sender;
        String subject = upset ? "Making things right, " + company : "Next steps for " + company;
        return new DraftResult(subject, body);
    }
}
