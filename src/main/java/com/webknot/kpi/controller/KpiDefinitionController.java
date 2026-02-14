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

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/kpi-definitions")
public class KpiDefinitionController {

    private final KpiDefinitionService kpiDefinitionService;
    private final Logger log = LogManager.getLogger(KpiDefinitionController.class);

    public KpiDefinitionController(KpiDefinitionService kpiDefinitionService) {
        this.kpiDefinitionService = kpiDefinitionService;
    }

    @GetMapping("/getall")
    public ResponseEntity<?> getAll() {
        try {
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
            log.error("Validation error while adding KPI definition: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while adding KPI definition: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody KpiDefinition def) {
        try {
            KpiDefinition updated = kpiDefinitionService.update(def);
            log.info("Successfully updated KPI definition with ID: " + updated.getId());
            return ResponseEntity.status(HttpStatus.OK).body(updated);
        } catch (CrudOperationException e) {
            log.error("Error while updating KPI definition: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (CrudValidationException e) {
            log.error("Validation error while updating KPI definition: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while updating KPI definition: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            kpiDefinitionService.delete(id);
            log.info("Successfully deleted KPI definition with ID: " + id);
            return ResponseEntity.status(HttpStatus.OK).body("KPI definition deleted successfully");
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
}
