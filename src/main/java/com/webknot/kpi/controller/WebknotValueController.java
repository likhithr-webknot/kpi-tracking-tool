package com.webknot.kpi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/webknot-values")
public class WebknotValueController {

    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestParam(required = false) Map<String, String> query) {
        return ResponseEntity.ok(Map.of("status", "ok", "action", "list", "query", query));
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "ok", "action", "add", "payload", body));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(Map.of("status", "ok", "action", "update", "id", id, "payload", body));
    }

    @PatchMapping("/update/{id}")
    public ResponseEntity<?> patch(@PathVariable String id, @RequestBody(required = false) Map<String, Object> body) {
        return update(id, body);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable String id) {
        return ResponseEntity.ok(Map.of("status", "ok", "action", "delete", "id", id));
    }
}
