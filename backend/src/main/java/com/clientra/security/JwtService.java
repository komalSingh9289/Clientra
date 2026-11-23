package com.clientra.security;

import com.clientra.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Issues and verifies JSON Web Tokens (synopsis 11.1).
 * The token is signed with HMAC-SHA256 using clientra.jwt.secret and expires after 60 minutes.
 * Because the signature proves the token is genuine, the server keeps NO session state.
 */
@Service
public class JwtService {
    private final SecretKey key;
    private final long expiryMinutes;

    public JwtService(@Value("${clientra.jwt.secret}") String secret,
                      @Value("${clientra.jwt.expiry-minutes:60}") long expiryMinutes) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("clientra.jwt.secret must be at least 32 characters long.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiryMinutes = expiryMinutes;
    }

    /** Called after a successful login. 'exp' is seconds-since-epoch as the JWT standard requires. */
    public String issue(User user, String roleName) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .claim("role", roleName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiryMinutes * 60)))
                .signWith(key)
                .compact();
    }

    /** Throws JwtException when the token is expired, tampered with, or malformed. */
    public Claims parse(String token) throws JwtException {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
