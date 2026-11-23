package com.clientra.security;

import com.clientra.exception.ApiException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Brute-force protection (synopsis 11.6): at most N failed logins per IP inside a rolling window
 * (default 5 per 15 minutes). The Nth failure blocks the IP for one window.
 * Kept in memory: simple and fine for a single server instance.
 */
@Component
public class LoginRateLimiter {
    private final int maxAttempts;
    private final long windowMillis;
    private final Map<String, Deque<Long>> failures = new HashMap<>();
    private final Map<String, Long> blockedUntil = new HashMap<>();

    public LoginRateLimiter(@Value("${clientra.security.login-max-attempts:5}") int maxAttempts,
                            @Value("${clientra.security.login-window-minutes:15}") int windowMinutes) {
        this.maxAttempts = maxAttempts;
        this.windowMillis = windowMinutes * 60_000L;
    }

    /** Call BEFORE checking the password. Throws 429 if the IP is currently blocked. */
    public synchronized void checkAllowed(String ip) {
        Long until = blockedUntil.get(ip);
        if (until == null) return;
        long now = System.currentTimeMillis();
        if (until > now) {
            long mins = Math.max(1, (until - now + 59_999) / 60_000);
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many failed login attempts. Try again in " + mins + " minute(s).", "RATE_LIMITED");
        }
        blockedUntil.remove(ip);   // block expired
    }

    /** Call after a wrong password. Starts a block when the limit is reached. */
    public synchronized void recordFailure(String ip) {
        long now = System.currentTimeMillis();
        Deque<Long> q = failures.computeIfAbsent(ip, k -> new ArrayDeque<>());
        while (!q.isEmpty() && now - q.peekFirst() > windowMillis) q.pollFirst();   // forget old failures
        q.addLast(now);
        if (q.size() >= maxAttempts) {
            blockedUntil.put(ip, now + windowMillis);
            q.clear();
        }
    }

    /** Call after a successful login: the IP starts clean. */
    public synchronized void recordSuccess(String ip) {
        failures.remove(ip);
        blockedUntil.remove(ip);
    }
}
