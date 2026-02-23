package com.webknot.kpi.controller;

import com.webknot.kpi.models.KpiDefinition;
import com.webknot.kpi.models.CurrentBand;
import com.webknot.kpi.models.CurrentStream;
import com.webknot.kpi.models.Employee;
import com.webknot.kpi.repository.EmployeeRepository;
import com.webknot.kpi.service.KpiDefinitionService;
import com.webknot.kpi.service.WebknotValueService;
import com.webknot.kpi.util.BandStreamNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/employee-portal")
public class EmployeePortalReadController {

    private static final Logger log = LoggerFactory.getLogger(EmployeePortalReadController.class);
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 100;

    private final KpiDefinitionService kpiDefinitionService;
    private final WebknotValueService webknotValueService;
    private final EmployeeRepository employeeRepository;

    public EmployeePortalReadController(KpiDefinitionService kpiDefinitionService,
                                        WebknotValueService webknotValueService,
                                        EmployeeRepository employeeRepository) {
        this.kpiDefinitionService = kpiDefinitionService;
        this.webknotValueService = webknotValueService;
        this.employeeRepository = employeeRepository;
    }

    @GetMapping("/kpi-definitions")
    public ResponseEntity<?> listKpiDefinitions(@RequestParam(required = false) Map<String, String> query,
                                                Authentication authentication) {
        int limit = parsePositiveInt(query.get("limit"), DEFAULT_LIMIT, MAX_LIMIT);
        int offset = parseNonNegativeInt(query.get("cursor"), 0);
        String employeeId = trimToNull(query.get("employeeId"));
        String explicitBand = trimToNull(query.get("band"));
        String explicitStream = trimToNull(query.get("stream"));

        CurrentBand band = BandStreamNormalizer.parseBand(explicitBand).orElse(null);
        CurrentStream stream = BandStreamNormalizer.parseStream(explicitStream).orElse(null);
        String resolvedBy = "query";

        if (band == null && stream == null) {
            Optional<Employee> employee = resolveEmployeeContext(employeeId, authentication);
            if (employee.isPresent()) {
                band = employee.get().getBand();
                stream = BandStreamNormalizer.parseStream(employee.get().getStream()).orElse(null);
                resolvedBy = employeeId != null ? "employeeId" : "auth";
            }
        }

        List<KpiDefinition> source = (band != null || stream != null)
                ? kpiDefinitionService.search(band, stream)
                : kpiDefinitionService.getAll();

        List<KpiDefinition> all = source.stream()
                .sorted(Comparator.comparing(KpiDefinition::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        int safeOffset = Math.min(offset, all.size());
        int end = Math.min(safeOffset + limit, all.size());
        List<KpiDefinition> items = all.subList(safeOffset, end);
        String nextCursor = end < all.size() ? String.valueOf(end) : null;

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("nextCursor", nextCursor);
        body.put("appliedBand", band != null ? band.name() : null);
        body.put("appliedStream", stream != null ? stream.name() : null);
        body.put("resolvedBy", resolvedBy);
        log.info("Employee portal KPI list resolvedBy={} band={} stream={} size={}",
                resolvedBy,
                band != null ? band.name() : null,
                stream != null ? stream.name() : null,
                items.size());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/webknot-values")
    public ResponseEntity<?> listWebknotValues(@RequestParam(required = false) Map<String, String> query) {
        int limit = parsePositiveInt(query.get("limit"), DEFAULT_LIMIT, MAX_LIMIT);
        String cursor = query.get("cursor");
        WebknotValueService.CursorPage page = webknotValueService.list(true, limit, cursor);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", page.items());
        body.put("nextCursor", page.nextCursor());
        return ResponseEntity.ok(body);
    }

    private Optional<Employee> resolveEmployeeContext(String employeeId, Authentication authentication) {
        if (employeeId != null) {
            return employeeRepository.findById(employeeId);
        }
        String email = authentication == null ? null : trimToNull(authentication.getName());
        if (email == null || "anonymousUser".equalsIgnoreCase(email)) return Optional.empty();
        return employeeRepository.findByEmail(email);
    }

    private static int parsePositiveInt(String raw, int defaultValue, int maxValue) {
        try {
            int value = Integer.parseInt(String.valueOf(raw == null ? "" : raw).trim());
            if (value <= 0) return defaultValue;
            return Math.min(value, maxValue);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static int parseNonNegativeInt(String raw, int defaultValue) {
        try {
            int value = Integer.parseInt(String.valueOf(raw == null ? "" : raw).trim());
            return Math.max(value, 0);
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    private static String trimToNull(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        return s.isEmpty() ? null : s;
    }
}
