package com.clientra.web;

import com.clientra.dto.Dtos.RulesDto;
import com.clientra.service.SettingsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** E-mail notification rules (Module 8). Everyone who can open Settings may read; only the admin may save. */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {
    private final SettingsService settings;

    public SettingsController(SettingsService settings) { this.settings = settings; }

    @GetMapping("/notification-rules")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR','SALES_MANAGER')")
    public RulesDto get() { return settings.get(); }

    @PutMapping("/notification-rules")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public RulesDto save(@RequestBody RulesDto r) { return settings.save(r); }
}
