package com.webknot.kpi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/monthly-submissions")
public class MonthlySubmissionsController {

    @GetMapping("/draft")
    public ResponseEntity<?> getDraft() {
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Draft fetched"));
    }

    @PostMapping("/draft")
    public ResponseEntity<?> saveDraft(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Draft saved", "payload", body));
    }

    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "ok", "message", "Submission received", "payload", body));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getMine(@RequestParam(required = false) Map<String, String> query) {
        return ResponseEntity.ok(Map.of("status", "ok", "scope", "me", "query", query));
    }

    @GetMapping("/me/history")
    public ResponseEntity<?> getMyHistory(@RequestParam(required = false) Map<String, String> query) {
        return ResponseEntity.ok(Map.of("status", "ok", "scope", "history", "query", query));
    }

    @GetMapping("/manager/team")
    public ResponseEntity<?> getManagerTeam(@RequestParam(required = false) Map<String, String> query) {
        return ResponseEntity.ok(Map.of("status", "ok", "scope", "manager-team", "query", query));
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> getAdminAll(@RequestParam(required = false) Map<String, String> query) {
        return ResponseEntity.ok(Map.of("status", "ok", "scope", "admin-all", "query", query));
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<?> getAdminById(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("status", "ok", "id", id));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<?> deleteAdminById(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("status", "ok", "action", "delete", "id", id));
    }
}
