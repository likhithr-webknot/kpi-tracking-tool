package com.webknot.kpi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portal")
public class PortalController {

    @GetMapping("/employee")
    public ResponseEntity<?> employeePortal() {
        return ResponseEntity.ok("Employee portal access granted");
    }

    @GetMapping("/manager")
    public ResponseEntity<?> managerPortal() {
        return ResponseEntity.ok("Manager portal access granted");
    }

    @GetMapping("/admin")
    public ResponseEntity<?> adminPortal() {
        return ResponseEntity.ok("Admin portal access granted");
    }
}
