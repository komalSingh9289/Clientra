package com.clientra.web;

import com.clientra.dto.Dtos.*;
import com.clientra.entity.Role;
import com.clientra.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** UC-13 Manage User Accounts. Administrator only, except /users/assignable which every role needs for pickers. */
@RestController
@RequestMapping("/api")
public class UserController {
    private final UserService users;

    public UserController(UserService users) { this.users = users; }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<UserDto> list() { return users.list(); }

    @GetMapping("/users/assignable")
    public List<AssignableUserDto> assignable() { return users.assignable(); }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public List<Role> roles() { return users.roles(); }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<UserDto> create(@Valid @RequestBody UserRequest r) { return ResponseEntity.status(201).body(users.create(r)); }

    @PutMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public UserDto update(@PathVariable Integer id, @Valid @RequestBody UserRequest r) { return users.update(id, r); }

    /** PATCH /api/users/{id}/active {active:true|false} */
    @PatchMapping("/users/{id}/active")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public UserDto active(@PathVariable Integer id, @RequestBody ActiveRequest r) { return users.setActive(id, r.active()); }
}
