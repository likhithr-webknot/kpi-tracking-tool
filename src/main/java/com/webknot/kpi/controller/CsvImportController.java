package com.webknot.kpi.controller;

import com.webknot.kpi.service.CsvImportService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/admin/imports")
public class CsvImportController {

    private final CsvImportService csvImportService;

    public CsvImportController(CsvImportService csvImportService) {
        this.csvImportService = csvImportService;
    }

    @PostMapping("/csv/{entity}")
    public ResponseEntity<?> importSingle(@PathVariable String entity,
                                          @RequestParam("file") MultipartFile file,
                                          @RequestParam(value = "validateOnly", defaultValue = "false") boolean validateOnly,
                                          @RequestParam(value = "rejectExisting", defaultValue = "false") boolean rejectExisting,
                                          Authentication authentication) {
        try {
            requireAdmin(authentication);
            CsvImportService.ImportOptions options = new CsvImportService.ImportOptions(validateOnly, rejectExisting);
            CsvImportService.ImportSummary summary = switch (normalize(entity)) {
                case "employees", "employeedirectory", "employee", "employeeimport" ->
                        csvImportService.importEmployees(file, options);
                case "bands", "banddirectory", "band" ->
                        csvImportService.importBands(file, options);
                case "streams", "streamdirectory", "stream" ->
                        csvImportService.importStreams(file, options);
                case "webknotvalues", "webknotvalue", "values" ->
                        csvImportService.importWebknotValues(file, options);
                case "kpidefinitions", "kpimasterregistry", "kpimasterregistery", "kpimaster", "kpi" ->
                        csvImportService.importKpiDefinitions(file, options);
                case "certifications", "certification" ->
                        csvImportService.importCertifications(file, options);
                case "designationlookups", "designationlookup", "designations", "designation" ->
                        csvImportService.importDesignationLookups(file, options);
                default -> throw new IllegalArgumentException(
                        "Unsupported import entity: " + entity +
                                ". Supported: employees, bands, streams, webknot-values, kpi-definitions, certifications, designation-lookups");
            };
            if (summary.totalRows() > 0 && summary.failed() == summary.totalRows()) {
                return ResponseEntity.badRequest().body(summary);
            }
            return ResponseEntity.ok(summary);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/csv/all")
    public ResponseEntity<?> importAll(@RequestParam(value = "designationLookups", required = false) MultipartFile designationLookups,
                                       @RequestParam(value = "bands", required = false) MultipartFile bands,
                                       @RequestParam(value = "streams", required = false) MultipartFile streams,
                                       @RequestParam(value = "webknotValues", required = false) MultipartFile webknotValues,
                                       @RequestParam(value = "kpiDefinitions", required = false) MultipartFile kpiDefinitions,
                                       @RequestParam(value = "certifications", required = false) MultipartFile certifications,
                                       @RequestParam(value = "employees", required = false) MultipartFile employees,
                                       @RequestParam(value = "validateOnly", defaultValue = "false") boolean validateOnly,
                                       @RequestParam(value = "rejectExisting", defaultValue = "false") boolean rejectExisting,
                                       Authentication authentication) {
        try {
            requireAdmin(authentication);
            CsvImportService.ImportOptions options = new CsvImportService.ImportOptions(validateOnly, rejectExisting);

            Map<String, CsvImportService.ImportSummary> results = new LinkedHashMap<>();

            if (designationLookups != null && !designationLookups.isEmpty()) {
                results.put("designationLookups", csvImportService.importDesignationLookups(designationLookups, options));
            }
            if (bands != null && !bands.isEmpty()) {
                results.put("bands", csvImportService.importBands(bands, options));
            }
            if (streams != null && !streams.isEmpty()) {
                results.put("streams", csvImportService.importStreams(streams, options));
            }
            if (webknotValues != null && !webknotValues.isEmpty()) {
                results.put("webknotValues", csvImportService.importWebknotValues(webknotValues, options));
            }
            if (kpiDefinitions != null && !kpiDefinitions.isEmpty()) {
                results.put("kpiDefinitions", csvImportService.importKpiDefinitions(kpiDefinitions, options));
            }
            if (certifications != null && !certifications.isEmpty()) {
                results.put("certifications", csvImportService.importCertifications(certifications, options));
            }
            if (employees != null && !employees.isEmpty()) {
                results.put("employees", csvImportService.importEmployees(employees, options));
            }

            if (results.isEmpty()) {
                throw new IllegalArgumentException(
                        "At least one CSV part is required. Use one or more of: designationLookups, bands, streams, webknotValues, kpiDefinitions, certifications, employees");
            }

            int totalRows = results.values().stream().mapToInt(CsvImportService.ImportSummary::totalRows).sum();
            int totalCreated = results.values().stream().mapToInt(CsvImportService.ImportSummary::created).sum();
            int totalUpdated = results.values().stream().mapToInt(CsvImportService.ImportSummary::updated).sum();
            int totalFailed = results.values().stream().mapToInt(CsvImportService.ImportSummary::failed).sum();

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("status", "completed");
            response.put("totalRows", totalRows);
            response.put("created", totalCreated);
            response.put("updated", totalUpdated);
            response.put("failed", totalFailed);
            response.put("results", results);
            if (totalRows > 0 && totalFailed == totalRows) {
                return ResponseEntity.badRequest().body(response);
            }
            return ResponseEntity.ok(response);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private static void requireAdmin(Authentication authentication) {
        boolean isAdmin = authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_Admin".equalsIgnoreCase(a.getAuthority()));
        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
