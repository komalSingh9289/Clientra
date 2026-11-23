package com.clientra.service;

import com.clientra.dto.Dtos.LoginResponse;
import com.clientra.dto.Dtos.UserDto;
import com.clientra.entity.Role;
import com.clientra.entity.User;
import com.clientra.exception.ApiException;
import com.clientra.repository.RoleRepository;
import com.clientra.repository.UserRepository;
import com.clientra.security.CurrentUser;
import com.clientra.security.JwtService;
import com.clientra.security.LoginRateLimiter;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Module 1 (Authentication). Login flow (diagram 7.5, first steps):
 *   rate-limit check -> find user -> bcrypt compare -> active check -> stamp last_login -> audit -> issue JWT.
 */
@Service
public class AuthService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginRateLimiter limiter;
    private final AuditService audit;
    private final UserService userService;
    private final CurrentUser current;
    /** A real bcrypt hash of a random string: compared when the e-mail is unknown so the response time doesn't reveal which e-mails exist. */
    private final String dummyHash;

    public AuthService(UserRepository users, RoleRepository roles, PasswordEncoder encoder, JwtService jwt,
                       LoginRateLimiter limiter, AuditService audit, UserService userService, CurrentUser current) {
        this.users = users; this.roles = roles; this.encoder = encoder; this.jwt = jwt; this.limiter = limiter;
        this.audit = audit; this.userService = userService; this.current = current;
        this.dummyHash = encoder.encode(java.util.UUID.randomUUID().toString());
    }

    /** noRollbackFor: failed-login audit rows must still be saved when we throw the 401. */
    @Transactional(noRollbackFor = ApiException.class)
    public LoginResponse login(String email, String password, String ip) {
        limiter.checkAllowed(ip);
        User u = users.findByEmailIgnoreCase(email.trim()).orElse(null);
        boolean ok = encoder.matches(password, u == null ? dummyHash : u.getPasswordHash());
        if (u == null || !ok) {
            limiter.recordFailure(ip);
            audit.logAs(u == null ? null : u.getUserId(), u == null ? "Unknown" : u.getFullName(), "LOGIN_FAILED",
                    "Account", "Failed login for " + email.trim() + " from " + ip);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.", "BAD_CREDENTIALS");
        }
        if (!u.isActive()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This account is deactivated. Contact your administrator.", "DEACTIVATED");
        }
        limiter.recordSuccess(ip);
        u.setLastLogin(Instant.now());
        users.save(u);
        Role role = roles.findById(u.getRoleId()).orElseThrow(() -> new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Account has no role."));
        audit.logAs(u.getUserId(), u.getFullName(), "LOGIN", "Session", "Signed in from " + ip);
        return new LoginResponse(jwt.issue(u, role.getRoleName()), userService.toDto(u, role.getRoleName()));
    }

    /** JWTs are stateless, so "logout" is client-side (the browser drops the token); we only record it. */
    public void logout() {
        audit.log("LOGOUT", "Session", "Signed out");
    }

    @Transactional(readOnly = true)
    public UserDto me() {
        User u = users.findById(current.id()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in."));
        return userService.toDto(u, current.get().role());
    }

    @Transactional
    public void changePassword(String currentPassword, String newPassword) {
        User u = users.findById(current.id()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in."));
        if (!encoder.matches(currentPassword, u.getPasswordHash())) throw ApiException.badRequest("Current password is incorrect.");
        u.setPasswordHash(encoder.encode(newPassword));
        users.save(u);
        audit.log("UPDATE", "User #" + u.getUserId(), "Changed own password");
    }
}
