package com.webknot.kpi.controller;


import com.webknot.kpi.dto.ScheduleWindowRequest;
import com.webknot.kpi.dto.SubmissionWindowResponse;
import com.webknot.kpi.service.SubmissionWindowService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/submission-window")
public class SubmissionWindowController {

    private final SubmissionWindowService service;
    private final Logger log = LogManager.getLogger(SubmissionWindowController.class);

    public SubmissionWindowController(SubmissionWindowService service) {
        this.service = service;
    }

    @GetMapping("/current")
    public SubmissionWindowResponse getCurrent() {
        log.info("Fetching current submission window");
        return service.getCurrentWindow();
    }

    @PreAuthorize("hasRole('Admin')")
    @PutMapping("/current/schedule")
    public SubmissionWindowResponse schedule(@Valid @RequestBody ScheduleWindowRequest req) {
        log.info("Scheduling submission window");
        return service.scheduleCurrentWindow(req, null);
    }

    @PreAuthorize("hasRole('Admin')")
    @PostMapping("/current/open-now")
    public SubmissionWindowResponse openNow() {
        log.info("Opening submission window immediately");
        return service.openNow(null);
    }

    @PreAuthorize("hasRole('Admin')")
    @PostMapping("/current/close-now")
    public SubmissionWindowResponse closeNow() {
        log.info("Closing submission window immediately");
        return service.closeNow(null);
    }
}
