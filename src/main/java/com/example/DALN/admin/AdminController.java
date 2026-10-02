package com.example.DALN.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) { this.adminService = adminService; }

    @GetMapping("/overview")
    public AdminService.Overview overview() { return adminService.overview(); }

    @GetMapping("/venues/pending")
    public List<AdminService.VenueRow> pendingVenues() { return adminService.pendingVenues(); }

    @PutMapping("/venues/{venueId}/decision")
    public AdminService.VenueDecision decideVenue(@PathVariable long venueId, @Valid @RequestBody VenueDecisionRequest request) {
        return adminService.decideVenue(venueId, request.status());
    }

    public record VenueDecisionRequest(@NotBlank String status) {}
}
