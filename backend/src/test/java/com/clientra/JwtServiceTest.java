package com.clientra;

import static org.junit.jupiter.api.Assertions.*;

import com.clientra.entity.User;
import com.clientra.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

/** Unit test (synopsis 10.6): valid tokens verify, tampered or foreign-signed tokens are rejected. */
class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-unit-test-secret-1234";

    private User user() {
        User u = new User();
        u.setUserId(7);
        u.setEmail("a@b.in");
        u.setFullName("A B");
        return u;
    }

    @Test void issuedTokenRoundTrips() {
        JwtService jwt = new JwtService(SECRET, 60);
        Claims c = jwt.parse(jwt.issue(user(), "Sales Manager"));
        assertEquals("7", c.getSubject());
        assertEquals("Sales Manager", c.get("role", String.class));
    }

    @Test void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 60);
        String t = jwt.issue(user(), "Administrator");
        assertThrows(JwtException.class, () -> jwt.parse(t.substring(0, t.length() - 3) + "abc"));
    }

    @Test void tokenSignedWithAnotherSecretIsRejected() {
        String foreign = new JwtService("another-secret-another-secret-123456", 60).issue(user(), "Administrator");
        assertThrows(JwtException.class, () -> new JwtService(SECRET, 60).parse(foreign));
    }

    @Test void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, -1);       // already expired when issued
        assertThrows(JwtException.class, () -> jwt.parse(jwt.issue(user(), "Administrator")));
    }

    @Test void shortSecretIsRefused() {
        assertThrows(IllegalStateException.class, () -> new JwtService("short", 60));
    }
}
