package com.webknot.kpi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/employee-portal")
public class EmployeePortalReadController {

    @GetMapping("/kpi-definitions")
    public ResponseEntity<?> listKpiDefinitions(@RequestParam(required = false) Map<String, String> query) {
        return ResponseEntity.ok(Map.of("status", "ok", "resource", "kpi-definitions", "query", query));
    }

    @GetMapping("/webknot-values")
    public ResponseEntity<?> listWebknotValues(@RequestParam(required = false) Map<String, String> query) {
        return ResponseEntity.ok(Map.of("status", "ok", "resource", "webknot-values", "query", query));
    }
}
