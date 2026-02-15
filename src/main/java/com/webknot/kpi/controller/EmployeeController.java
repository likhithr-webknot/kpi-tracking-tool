package com.webknot.kpi.controller;

import com.webknot.kpi.exceptions.CrudOperationException;
import com.webknot.kpi.exceptions.CrudValidationException;
import com.webknot.kpi.models.Employee;
import com.webknot.kpi.service.EmployeeService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final Logger log = LogManager.getLogger(EmployeeController.class);

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/getall")
    public ResponseEntity<?> getAllEmployees() {
        try {
            List<Employee> employees = employeeService.getAllEmployees();
            log.info("Successfully fetched " + employees.size() + " employees");

            List<EmployeeResponse> response = employees.stream()
                    .map(EmployeeController::toResponse)
                    .toList();

            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (CrudOperationException e) {
            log.error("Error while fetching employees: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (CrudValidationException e) {
            log.error("Validation error while fetching employees: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while fetching employees: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> getEmployeeById(@PathVariable("id") String id) {
        try {
            Employee employee = employeeService.getEmployeeById(id)
                    .orElse(null);

            if (employee == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found: " + id);
            }

            log.info("Successfully fetched employee with ID: " + id);
            return ResponseEntity.status(HttpStatus.OK).body(toResponse(employee));
        } catch (CrudOperationException e) {
            log.error("Error while fetching employee: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (CrudValidationException e) {
            log.error("Validation error while fetching employee: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while fetching employee: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addEmployee(@RequestBody Employee employee) {
        try {
            Employee saved = employeeService.addEmployee(employee);
            log.info("Successfully added employee with ID: " + saved.getEmployeeId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
        } catch (CrudOperationException e) {
            log.error("Error while adding employee: " + e.getMessage(), e);
            String errorMessage = e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " - Cause: " + e.getCause().getMessage();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMessage);
        } catch (CrudValidationException e) {
            log.error("Validation error while adding employee: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while adding employee: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/promote")
    public ResponseEntity<?> promoteEmployee(@PathVariable("id") String id) {
        try {
            Employee employee = employeeService.promoteEmployee(id).orElse(null);
            if (employee == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found: " + id);
            }

            log.info("Successfully promoted employee with ID: " + id);
            return ResponseEntity.status(HttpStatus.OK).body(toResponse(employee));
        } catch (CrudOperationException e) {
            log.error("Error while promoting employee: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (CrudValidationException e) {
            log.error("Validation error while promoting employee: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while promoting employee: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    private static EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(
                e.getEmployeeId(),
                e.getEmployeeName(),
                e.getEmail(),
                e.getEmpRole() != null ? e.getEmpRole().name() : null,
                e.getStream(),
                e.getBand() != null ? e.getBand().name() : null,
                e.getManager() != null ? e.getManager().getEmployeeId() : null,
                e.getUpdatedBy() != null ? e.getUpdatedBy().getEmployeeId() : null,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public record EmployeeResponse(
            String employeeId,
            String employeeName,
            String email,
            String empRole,
            String stream,
            String band,
            String managerId,
            String updatedById,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {}
}
