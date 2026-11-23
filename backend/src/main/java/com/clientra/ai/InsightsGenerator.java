package com.clientra.ai;

import com.clientra.dto.Dtos.Insight;
import com.clientra.dto.Dtos.LeadDto;
import com.clientra.dto.Dtos.TrendPoint;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Dashboard anomaly / trend call-outs (Module 7: "the AI layer surfaces anomalies and trend predictions"). */
@Component
public class InsightsGenerator {

    public List<Insight> generate(List<LeadDto> stalledHighPriority, long negativeCount, List<TrendPoint> trend) {
        List<Insight> out = new ArrayList<>();
        for (LeadDto l : stalledHighPriority) {
            out.add(new Insight("warning", l.fullName() + " (" + l.company() + ") is a high-priority lead with no activity for "
                    + l.idleDays() + " days.", l.leadId()));
        }
        if (negativeCount > 0) {
            out.add(new Insight("warning", negativeCount + " lead" + (negativeCount > 1 ? "s show" : " shows")
                    + " negative sentiment and need attention.", null));
        }
        if (trend.size() >= 2) {
            double prev = trend.get(trend.size() - 2).rate();
            double cur = trend.get(trend.size() - 1).rate();
            boolean up = cur >= prev;
            double forecast = Math.max(0, Math.min(100, cur + (cur - prev) / 2));   // simple linear extrapolation
            out.add(new Insight(up ? "success" : "info", String.format(Locale.ROOT,
                    "Conversion rate is %s %.1f points vs last month. Forecast next month: %.1f%%.",
                    up ? "up" : "down", Math.abs(cur - prev), forecast), null));
        }
        return out;
    }
}
