package com.clientra.web;

import com.clientra.dto.Dtos.ActivityDto;
import com.clientra.dto.Dtos.ActivityRequest;
import com.clientra.dto.Dtos.ActivityResult;
import com.clientra.service.ActivityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

/** UC-04 Log Interaction / UC-06 view history. */
@RestController
@RequestMapping("/api/activities")
public class ActivityController {
    private final ActivityService activities;

    public ActivityController(ActivityService activities) { this.activities = activities; }

    /** GET /api/activities?leadId= */
    @GetMapping
    public List<ActivityDto> list(@RequestParam(required = false) Integer leadId) { return activities.list(leadId); }

    /** POST /api/activities : returns {activity, sentiment, flagged, score}. */
    @PostMapping
    public ActivityResult create(@Valid @RequestBody ActivityRequest r) { return activities.create(r); }
}
