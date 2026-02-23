package com.webknot.kpi.controller;

import com.webknot.kpi.exceptions.CrudOperationException;
import com.webknot.kpi.exceptions.CrudValidationException;
import com.webknot.kpi.models.CurrentBand;
import com.webknot.kpi.models.CurrentStream;
import com.webknot.kpi.models.KpiDefinition;
import com.webknot.kpi.service.KpiDefinitionService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping({"/kpi-definitions", "/kpi-definition"})
public class KpiDefinitionController {

    private final KpiDefinitionService kpiDefinitionService;
    private final Logger log = LogManager.getLogger(KpiDefinitionController.class);

    public KpiDefinitionController(KpiDefinitionService kpiDefinitionService) {
        this.kpiDefinitionService = kpiDefinitionService;
    }

    private String toValidationResponse(CrudValidationException e) {
        if (e.hasViolations()) {
            return e.getViolationsAsString();
        }
        return e.getMessage();
    }

    @GetMapping("/getall")
    public ResponseEntity<?> getAll(@RequestParam(required = false) Integer limit,
                                    @RequestParam(required = false) String cursor) {
        try {
            boolean paginationRequested = limit != null || (cursor != null && !cursor.isBlank());
            if (paginationRequested) {
                KpiDefinitionService.KpiCursorPage page = kpiDefinitionService.getAllCursorPage(limit, cursor);
                return ResponseEntity.status(HttpStatus.OK).body(new CursorPageResponse<>(page.items(), page.nextCursor()));
            }

            List<KpiDefinition> defs = kpiDefinitionService.getAll();
            log.info("Successfully fetched " + defs.size() + " KPI definitions");
            return ResponseEntity.status(HttpStatus.OK).body(defs);
        } catch (CrudOperationException e) {
            log.error("Error while fetching KPI definitions: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (CrudValidationException e) {
            log.error("Validation error while fetching KPI definitions: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while fetching KPI definitions: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        try {
            Optional<KpiDefinition> def = kpiDefinitionService.getById(id);
            log.info("Successfully fetched KPI definition with ID: " + id);
            return ResponseEntity.status(HttpStatus.OK).body(def);
        } catch (CrudOperationException e) {
            log.error("Error while fetching KPI definition: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (CrudValidationException e) {
            log.error("Validation error while fetching KPI definition: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while fetching KPI definition: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestBody KpiDefinition def) {
        try {
            KpiDefinition saved = kpiDefinitionService.add(def);
            log.info("Successfully added KPI definition with ID: " + saved.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (CrudOperationException e) {
            log.error("Error while adding KPI definition: " + e.getMessage(), e);
            String errorMessage = e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " - Cause: " + e.getCause().getMessage();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        } catch (CrudValidationException e) {
            String validationDetails = toValidationResponse(e);
            log.error("Validation error while adding KPI definition: " + validationDetails, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationDetails);
        } catch (Exception e) {
            log.error("Unexpected error while adding KPI definition: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/update", method = {RequestMethod.PUT, RequestMethod.POST})
    public ResponseEntity<?> update(@RequestBody KpiDefinition def) {
        try {
            KpiDefinition updated = kpiDefinitionService.update(def);
            log.info("Successfully updated KPI definition with ID: " + updated.getId());
            return ResponseEntity.status(HttpStatus.OK).body(updated);
        } catch (CrudOperationException e) {
            log.error("Error while updating KPI definition: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (CrudValidationException e) {
            String validationDetails = toValidationResponse(e);
            log.error("Validation error while updating KPI definition: " + validationDetails, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationDetails);
        } catch (Exception e) {
            log.error("Unexpected error while updating KPI definition: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @RequestMapping(value = {"/update/{id}", "/edit/{id}"}, method = {RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<?> updateById(@PathVariable Long id, @RequestBody KpiDefinition def) {
        try {
            if (id == null || id <= 0) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid KPI Definition id. Must be > 0");
            }
            if (def == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("KPI definition payload is required");
            }
            if (def.getId() != null && !id.equals(def.getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Path id and payload id mismatch. Use one KPI id consistently.");
            }
            def.setId(id);

            KpiDefinition updated = kpiDefinitionService.update(def);
            log.info("Successfully updated KPI definition with ID: " + updated.getId());
            return ResponseEntity.status(HttpStatus.OK).body(updated);
        } catch (CrudOperationException e) {
            log.error("Error while updating KPI definition by id: " + e.getMessage(), e);
            String errorMessage = e.getMessage();
            if (errorMessage != null && (errorMessage.contains("not found") || errorMessage.contains("Entity not found"))) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        } catch (CrudValidationException e) {
            String validationDetails = toValidationResponse(e);
            log.error("Validation error while updating KPI definition by id: " + validationDetails, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(validationDetails);
        } catch (Exception e) {
            log.error("Unexpected error while updating KPI definition by id: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            kpiDefinitionService.delete(id);
            log.info("Successfully deleted KPI definition with ID: " + id);
            LinkedHashMap<String, Object> body = new LinkedHashMap<>();
            body.put("status", "ok");
            body.put("id", id);
            body.put("message", "KPI definition deleted successfully");
            return ResponseEntity.status(HttpStatus.OK).body(body);
        } catch (CrudOperationException e) {
            log.error("Error while deleting KPI definition: " + e.getMessage(), e);
            String errorMessage = e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " - Cause: " + e.getCause().getMessage();
            }
            if (errorMessage.contains("not found") || errorMessage.contains("Entity not found")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        } catch (CrudValidationException e) {
            log.error("Validation error while deleting KPI definition: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while deleting KPI definition: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(
            @RequestParam(required = false) CurrentBand band,
            @RequestParam(required = false) CurrentStream stream
    ) {
        try {
            List<KpiDefinition> defs = kpiDefinitionService.search(band, stream);
            log.info("Successfully searched KPI definitions. Count=" + defs.size());
            return ResponseEntity.status(HttpStatus.OK).body(defs);
        } catch (CrudOperationException e) {
            log.error("Error while searching KPI definitions: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (CrudValidationException e) {
            log.error("Validation error while searching KPI definitions: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while searching KPI definitions: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    public record CursorPageResponse<T>(List<T> items, String nextCursor) {}
}
