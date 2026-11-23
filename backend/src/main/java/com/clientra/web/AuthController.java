package com.clientra.web;

import com.clientra.dto.Dtos.*;
import com.clientra.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** UC-01 Login / Logout. Only /login is public; everything else needs a valid JWT. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) { this.auth = auth; }

    /** POST /api/auth/login {email, password} -> {token, user} */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest r, HttpServletRequest req) {
        return auth.login(r.email(), r.password(), req.getRemoteAddr());
    }

    /** GET /api/auth/me : who am I? (used by the app on page reload) */
    @GetMapping("/me")
    public UserDto me() { return auth.me(); }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        auth.logout();
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest r) {
        auth.changePassword(r.currentPassword(), r.newPassword());
        return ResponseEntity.noContent().build();
    }
}
