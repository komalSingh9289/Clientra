package com.clientra.web;

import com.clientra.dto.Dtos.ReminderDto;
import com.clientra.dto.Dtos.ReminderRequest;
import com.clientra.dto.Dtos.StatusRequest;
import com.clientra.service.ReminderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** UC-08 Set Follow-up Reminder and the "My Tasks" list. */
@RestController
@RequestMapping("/api/reminders")
public class ReminderController {
    private final ReminderService reminders;

    public ReminderController(ReminderService reminders) { this.reminders = reminders; }

    @GetMapping
    public List<ReminderDto> list() { return reminders.list(); }

    @PostMapping
    public ResponseEntity<ReminderDto> create(@Valid @RequestBody ReminderRequest r) { return ResponseEntity.status(201).body(reminders.create(r)); }

    /** PATCH /api/reminders/{id} {status: Pending|Completed|Cancelled} */
    @PatchMapping("/{id}")
    public ReminderDto status(@PathVariable Integer id, @Valid @RequestBody StatusRequest r) { return reminders.setStatus(id, r.status()); }
}
