package com.clientra;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.clientra.exception.ApiException;
import com.clientra.security.LoginRateLimiter;
import org.junit.jupiter.api.Test;

/** Unit test (synopsis 11.6): 5 failed logins per IP block that IP; other IPs and a success reset are unaffected. */
class LoginRateLimiterTest {
    @Test void blocksAfterFiveFailures() {
        LoginRateLimiter l = new LoginRateLimiter(5, 15);
        for (int i = 0; i < 5; i++) { l.checkAllowed("1.1.1.1"); l.recordFailure("1.1.1.1"); }
        assertThrows(ApiException.class, () -> l.checkAllowed("1.1.1.1"));
        assertDoesNotThrow(() -> l.checkAllowed("2.2.2.2"));
        l.recordSuccess("1.1.1.1");
        assertDoesNotThrow(() -> l.checkAllowed("1.1.1.1"));
    }
}
