package com.clientra.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** Time helpers. All instants are stored in UTC; "day" boundaries are computed in the business time zone. */
public final class TimeUtil {
    private TimeUtil() {}
    /** The business time zone (India). Used for "start of day" filters, weekday names and the weekly digest. */
    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    /** Whole days between then and now (never negative). */
    public static long daysBetween(Instant then, Instant now) {
        if (then == null) return 0;
        return Math.max(0, Duration.between(then, now).toDays());
    }

    public static Instant startOfDay(LocalDate d) { return d.atStartOfDay(ZONE).toInstant(); }
    public static Instant endOfDay(LocalDate d) { return d.plusDays(1).atStartOfDay(ZONE).toInstant().minusMillis(1); }
}
