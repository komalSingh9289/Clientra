package com.clientra.ai;

import com.clientra.ai.AiModels.FollowUpResult;
import com.clientra.entity.Activity;
import com.clientra.util.TimeUtil;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;

/** Predictive follow-up timing: the weekday and 2-hour window in which this lead has engaged most often. */
@Component
public class FollowUpPredictor {

    public FollowUpResult best(List<Activity> activities) {
        if (activities.size() < 3) {
            return new FollowUpResult("Tuesday", "10:00 – 12:00",
                    "Not enough history yet; this is a general best-practice slot.");
        }
        Map<String, Integer> days = new LinkedHashMap<>();
        Map<Integer, Integer> hours = new TreeMap<>();
        for (Activity a : activities) {
            ZonedDateTime z = a.getActivityDate().atZone(TimeUtil.ZONE);
            days.merge(z.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH), 1, Integer::sum);
            hours.merge(z.getHour(), 1, Integer::sum);
        }
        String day = topKey(days);
        int h = topKey(hours);
        String window = String.format("%02d:00 – %02d:00", h, (h + 2) % 24);
        return new FollowUpResult(day, window, "Based on when this lead has engaged most often.");
    }

    /** Key with the highest count; the first one wins a tie. */
    private <K> K topKey(Map<K, Integer> m) {
        K best = null;
        int max = -1;
        for (Map.Entry<K, Integer> e : m.entrySet()) {
            if (e.getValue() > max) { max = e.getValue(); best = e.getKey(); }
        }
        return best;
    }
}
