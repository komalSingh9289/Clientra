package com.clientra.service;

import com.clientra.dto.Dtos.AssignableUserDto;
import com.clientra.dto.Dtos.UserDto;
import com.clientra.dto.Dtos.UserRequest;
import com.clientra.entity.Role;
import com.clientra.entity.User;
import com.clientra.exception.ApiException;
import com.clientra.repository.RoleRepository;
import com.clientra.repository.UserRepository;
import com.clientra.security.CurrentUser;
import com.clientra.security.Roles;
import java.time.Instant;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Module 8 / UC-13: administrators create, edit, re-role and deactivate accounts. */
@Service
public class UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final CurrentUser current;

    public UserService(UserRepository users, RoleRepository roles, PasswordEncoder encoder, AuditService audit, CurrentUser current) {
        this.users = users; this.roles = roles; this.encoder = encoder; this.audit = audit; this.current = current;
    }

    @Transactional(readOnly = true)
    public List<UserDto> list() {
        Map<Integer, String> roleNames = roleNames();
        List<UserDto> out = new ArrayList<>();
        for (User u : users.findAll()) out.add(toDto(u, roleNames.get(u.getRoleId())));
        out.sort(Comparator.comparing(UserDto::fullName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    /** Active users for "assign to" pickers; open to every signed-in role. */
    @Transactional(readOnly = true)
    public List<AssignableUserDto> assignable() {
        Map<Integer, String> roleNames = roleNames();
        List<AssignableUserDto> out = new ArrayList<>();
        for (User u : users.findAll()) {
            if (u.isActive()) out.add(new AssignableUserDto(u.getUserId(), u.getFullName(), roleNames.get(u.getRoleId()), true));
        }
        out.sort(Comparator.comparing(AssignableUserDto::fullName, String.CASE_INSENSITIVE_ORDER));
        return out;
    }

    @Transactional(readOnly = true)
    public List<Role> roles() { return roles.findAll(); }

    @Transactional
    public UserDto create(UserRequest r) {
        if (r.password() == null || r.password().length() < 8) throw ApiException.badRequest("Password must be at least 8 characters.");
        String email = r.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("That email is already registered.", "DUPLICATE");
        Role role = requireRole(r.roleId());
        User u = new User();
        u.setCompanyId(users.findById(current.id()).map(User::getCompanyId).orElse(1));   // same company as the admin
        u.setRoleId(role.getRoleId());
        u.setFullName(r.fullName().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setActive(true);
        u.setCreatedAt(Instant.now());
        users.save(u);
        audit.log("CREATE", "User #" + u.getUserId(), "Created " + role.getRoleName() + " account for " + u.getFullName());
        return toDto(u, role.getRoleName());
    }

    @Transactional
    public UserDto update(Integer id, UserRequest r) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found."));
        String email = r.email().trim().toLowerCase(Locale.ROOT);
        Optional<User> clash = users.findByEmailIgnoreCase(email);
        if (clash.isPresent() && !clash.get().getUserId().equals(id)) throw ApiException.conflict("That email is already registered.", "DUPLICATE");

        Role newRole = requireRole(r.roleId());
        Role oldRole = roles.findById(u.getRoleId()).orElse(null);
        boolean roleChanged = !newRole.getRoleId().equals(u.getRoleId());
        if (roleChanged && oldRole != null && Roles.ADMIN.equals(oldRole.getRoleName())) guardLastAdmin(u);

        u.setFullName(r.fullName().trim());
        u.setEmail(email);
        u.setRoleId(newRole.getRoleId());
        if (r.password() != null && !r.password().isBlank()) {          // optional admin password reset
            if (r.password().length() < 8) throw ApiException.badRequest("Password must be at least 8 characters.");
            u.setPasswordHash(encoder.encode(r.password()));
            audit.log("UPDATE", "User #" + id, "Password reset for " + u.getFullName());
        }
        users.save(u);
        if (roleChanged) {
            audit.log("ROLE_CHANGE", "User #" + id, u.getFullName() + ": " + (oldRole == null ? "?" : oldRole.getRoleName()) + " -> " + newRole.getRoleName());
        } else {
            audit.log("UPDATE", "User #" + id, "Updated account of " + u.getFullName());
        }
        return toDto(u, newRole.getRoleName());
    }

    @Transactional
    public UserDto setActive(Integer id, boolean active) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User not found."));
        if (!active) {
            if (u.getUserId().equals(current.id())) throw ApiException.badRequest("You cannot deactivate your own account.");
            Role r = roles.findById(u.getRoleId()).orElse(null);
            if (r != null && Roles.ADMIN.equals(r.getRoleName())) guardLastAdmin(u);
        }
        u.setActive(active);
        users.save(u);
        audit.log("UPDATE", "User #" + id, (active ? "Reactivated " : "Deactivated ") + u.getFullName());
        return toDto(u, roles.findById(u.getRoleId()).map(Role::getRoleName).orElse(""));
    }

    /** The system must always keep at least one active administrator. */
    private void guardLastAdmin(User admin) {
        Role adminRole = roles.findByRoleName(Roles.ADMIN).orElse(null);
        if (adminRole == null || !admin.isActive()) return;
        if (users.countByRoleIdAndActiveTrue(adminRole.getRoleId()) <= 1) {
            throw ApiException.badRequest("There must be at least one active administrator.");
        }
    }

    private Role requireRole(Integer id) {
        return roles.findById(id).orElseThrow(() -> ApiException.badRequest("That role does not exist."));
    }

    private Map<Integer, String> roleNames() {
        Map<Integer, String> m = new HashMap<>();
        for (Role r : roles.findAll()) m.put(r.getRoleId(), r.getRoleName());
        return m;
    }

    public UserDto toDto(User u, String roleName) {
        return new UserDto(u.getUserId(), u.getCompanyId(), u.getRoleId(), roleName, u.getFullName(), u.getEmail(),
                u.isActive(), u.getCreatedAt(), u.getLastLogin());
    }
}
