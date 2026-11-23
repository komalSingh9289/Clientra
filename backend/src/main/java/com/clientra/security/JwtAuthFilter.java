package com.clientra.security;

import com.clientra.entity.Role;
import com.clientra.entity.User;
import com.clientra.repository.RoleRepository;
import com.clientra.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Runs before every controller.
 *   1. Reads "Authorization: Bearer <token>".
 *   2. Verifies signature + expiry (JwtService). Expired / tampered tokens are simply ignored,
 *      which makes the request anonymous, so Spring answers 401.
 *   3. Re-loads the user from the DB so that a deactivated account or a changed role takes
 *      effect immediately, even while an old token is still valid.
 *   4. Puts an AuthUser + the role authority into the SecurityContext for RBAC checks.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserRepository users;
    private final RoleRepository roles;

    public JwtAuthFilter(JwtService jwt, UserRepository users, RoleRepository roles) {
        this.jwt = jwt;
        this.users = users;
        this.roles = roles;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwt.parse(header.substring(7));
                User user = users.findById(Integer.valueOf(claims.getSubject())).orElse(null);
                if (user != null && user.isActive()) {
                    Role role = roles.findById(user.getRoleId()).orElse(null);
                    if (role != null) {
                        AuthUser principal = new AuthUser(user.getUserId(), user.getEmail(), user.getFullName(), role.getRoleName());
                        var auth = new UsernamePasswordAuthenticationToken(principal, null,
                                List.of(new SimpleGrantedAuthority(Roles.authority(role.getRoleName()))));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();   // bad token -> stay anonymous -> 401
            }
        }
        chain.doFilter(req, res);
    }
}
