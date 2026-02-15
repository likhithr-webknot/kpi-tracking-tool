package com.webknot.kpi.controller;


import com.webknot.kpi.dto.ScheduleWindowRequest;
import com.webknot.kpi.dto.SubmissionWindowResponse;
import com.webknot.kpi.service.SubmissionWindowService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/submission-window")
public class SubmissionWindowController {

    private final SubmissionWindowService service;

    public SubmissionWindowController(SubmissionWindowService service) {
        this.service = service;
    }

    @GetMapping("/current")
    public SubmissionWindowResponse getCurrent() {
        return service.getCurrentWindow();
    }

    // Adjust this depending on your authority mapping:
    // - some setups need hasAuthority('Admin')
    // - some need hasRole('Admin') (implies ROLE_Admin)
    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/current/schedule")
    public SubmissionWindowResponse schedule(@Valid @RequestBody ScheduleWindowRequest req) {
        return service.scheduleCurrentWindow(req, null);
    }

    @PreAuthorize("hasRole('Admin')")
    @PostMapping("/current/open-now")
    public SubmissionWindowResponse openNow() {
        return service.openNow(null);
    }

    @PreAuthorize("hasRole('Admin')")
    @PostMapping("/current/close-now")
    public SubmissionWindowResponse closeNow() {
        return service.closeNow(null);
    }
}