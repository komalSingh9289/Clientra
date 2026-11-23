package com.clientra.web;

import com.clientra.dto.Dtos.DealDto;
import com.clientra.dto.Dtos.DealRequest;
import com.clientra.dto.Dtos.StatusRequest;
import com.clientra.service.DealService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deals")
public class DealController {
    private final DealService deals;

    public DealController(DealService deals) { this.deals = deals; }

    /** GET /api/deals?leadId= */
    @GetMapping
    public List<DealDto> byLead(@RequestParam Integer leadId) { return deals.byLead(leadId); }

    @PostMapping
    public ResponseEntity<DealDto> create(@Valid @RequestBody DealRequest r) { return ResponseEntity.status(201).body(deals.create(r)); }

    /** PATCH /api/deals/{id} {status: Open|Won|Lost} */
    @PatchMapping("/{id}")
    public DealDto status(@PathVariable Integer id, @Valid @RequestBody StatusRequest r) { return deals.setStatus(id, r.status()); }
}
