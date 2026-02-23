package com.webknot.kpi.controller;

import com.webknot.kpi.exceptions.CrudOperationException;
import com.webknot.kpi.exceptions.CrudValidationException;
import com.webknot.kpi.models.CurrentBand;
import com.webknot.kpi.models.DesignationLookup;
import com.webknot.kpi.models.Employee;
import com.webknot.kpi.models.EmployeeRole;
import com.webknot.kpi.repository.DesignationLookupRepository;
import com.webknot.kpi.service.EmployeeService;
import com.webknot.kpi.util.BandStreamNormalizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final DesignationLookupRepository designationLookupRepository;
    private final String defaultEmployeePassword;
    private final Logger log = LogManager.getLogger(EmployeeController.class);

    public EmployeeController(EmployeeService employeeService,
                              DesignationLookupRepository designationLookupRepository,
                              @Value("${employee.default-password:Password@123}") String defaultEmployeePassword) {
        this.employeeService = employeeService;
        this.designationLookupRepository = designationLookupRepository;
        this.defaultEmployeePassword = defaultEmployeePassword;
    }

    @GetMapping("/getall")
    public ResponseEntity<?> getAllEmployees(@RequestParam(required = false) Integer limit,
                                             @RequestParam(required = false) String cursor) {
        try {
            boolean paginationRequested = limit != null || (cursor != null && !cursor.isBlank());
            if (paginationRequested) {
                EmployeeService.EmployeeCursorPage page = employeeService.getEmployeesCursorPage(limit, cursor);
                List<EmployeeResponse> items = page.items().stream()
                        .map(this::toResponse)
                        .toList();
                return ResponseEntity.status(HttpStatus.OK).body(
                        new CursorPageResponse<>(
                                items,
                                page.nextCursor(),
                                page.total(),
                                page.managerCount(),
                                page.adminCount(),
                                page.employeeCount(),
                                page.bandCount()
                        )
                );
            }

            List<Employee> employees = employeeService.getAllEmployees();
            log.info("Successfully fetched " + employees.size() + " employees");

            List<EmployeeResponse> response = employees.stream()
                    .map(this::toResponse)
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

    @PostMapping("/add-with-manager")
    public ResponseEntity<?> addEmployeeWithManager(@RequestBody AddEmployeeWithManagerRequest request) {
        try {
            Employee employee = new Employee();
            employee.setEmployeeId(request.employeeId());
            employee.setEmployeeName(request.employeeName());
            employee.setEmail(request.email());
            employee.setEmpRole(parseRole(request.empRole()));
            employee.setStream(request.stream());
            employee.setBand(parseBand(request.band()));
            employee.setPassword(request.password());

            Employee saved = employeeService.addEmployeeWithManager(
                    employee,
                    request.managerId(),
                    defaultEmployeePassword
            );
            log.info("Successfully added employee with manager, ID: " + saved.getEmployeeId());
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
        } catch (IllegalArgumentException e) {
            log.error("Validation error while adding employee with manager: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (CrudOperationException e) {
            log.error("Error while adding employee with manager: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (CrudValidationException e) {
            log.error("Validation error while adding employee with manager: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while adding employee with manager: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @RequestMapping(value = {"/{id}/edit", "/edit/{id}", "/update/{id}"}, method = {RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.POST})
    public ResponseEntity<?> updateEmployee(@PathVariable("id") String id,
                                            @RequestBody(required = false) UpdateEmployeeRequest request) {
        try {
            if (request == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Employee update payload is required.");
            }

            EmployeeService.EmployeeUpdateCommand command = new EmployeeService.EmployeeUpdateCommand(
                    trimToNull(request.employeeId()),
                    trimToNull(request.employeeName()),
                    trimToNull(request.email()),
                    parseRoleNullable(request.empRole()),
                    trimToNull(request.stream()),
                    parseBand(request.band()),
                    request.managerId(),
                    request.updatedById(),
                    request.password()
            );

            Employee updated = employeeService.updateEmployee(id, command).orElse(null);
            if (updated == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found: " + id);
            }

            log.info("Successfully updated employee with ID: {}", id);
            return ResponseEntity.status(HttpStatus.OK).body(toResponse(updated));
        } catch (IllegalArgumentException e) {
            log.error("Validation error while updating employee {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (CrudValidationException e) {
            log.error("Validation error while updating employee {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (CrudOperationException e) {
            log.error("Error while updating employee {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while updating employee {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping({"/delete/{id}", "/{id}"})
    public ResponseEntity<?> deleteEmployee(@PathVariable("id") String id) {
        try {
            boolean deleted = employeeService.deleteEmployee(id);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Employee not found: " + id);
            }
            log.info("Successfully deleted employee with ID: {}", id);
            LinkedHashMap<String, Object> body = new LinkedHashMap<>();
            body.put("status", "ok");
            body.put("id", id);
            body.put("message", "Employee deleted successfully");
            return ResponseEntity.status(HttpStatus.OK).body(body);
        } catch (CrudValidationException e) {
            log.error("Validation error while deleting employee {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (CrudOperationException e) {
            log.error("Error while deleting employee {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while deleting employee {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/managers")
    public ResponseEntity<?> getManagers() {
        try {
            List<EmployeeResponse> response = employeeService.getManagers().stream()
                    .map(this::toResponse)
                    .toList();
            log.info("Successfully fetched " + response.size() + " managers");
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (CrudOperationException e) {
            log.error("Error while fetching managers: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while fetching managers: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/manager/{managerId}/reportees")
    public ResponseEntity<?> getReportees(@PathVariable("managerId") String managerId) {
        try {
            List<EmployeeResponse> response = employeeService.getReporteesByManagerId(managerId).stream()
                    .map(this::toResponse)
                    .toList();
            log.info("Successfully fetched " + response.size() + " reportees for manager " + managerId);
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (CrudOperationException e) {
            log.error("Error while fetching reportees: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } catch (CrudValidationException e) {
            log.error("Validation error while fetching reportees: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error while fetching reportees: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
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

    private EmployeeResponse toResponse(Employee e) {
        return new EmployeeResponse(
                e.getEmployeeId(),
                e.getEmployeeName(),
                e.getEmail(),
                e.getEmpRole() != null ? e.getEmpRole().name() : null,
                resolveDesignation(e),
                e.getStream(),
                e.getBand() != null ? e.getBand().name() : null,
                e.getManager() != null ? e.getManager().getEmployeeId() : null,
                e.getUpdatedBy() != null ? e.getUpdatedBy().getEmployeeId() : null,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    private String resolveDesignation(Employee e) {
        String stream = e != null ? e.getStream() : null;
        CurrentBand band = e != null ? e.getBand() : null;
        if (stream == null || stream.isBlank() || band == null) return null;
        String canonicalStream = BandStreamNormalizer.canonicalStreamLabel(stream);
        if (canonicalStream != null && !canonicalStream.isBlank()) {
            DesignationLookup.DesignationId canonicalId = new DesignationLookup.DesignationId(canonicalStream, band);
            var canonical = designationLookupRepository.findById(canonicalId).map(DesignationLookup::getDesignation);
            if (canonical.isPresent()) return canonical.get();
        }
        DesignationLookup.DesignationId rawId = new DesignationLookup.DesignationId(stream, band);
        return designationLookupRepository.findById(rawId).map(DesignationLookup::getDesignation).orElse(null);
    }

    private static EmployeeRole parseRole(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return EmployeeRole.Employee;
        for (EmployeeRole role : EmployeeRole.values()) {
            if (role.name().equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid employee role: " + value);
    }

    private static EmployeeRole parseRoleNullable(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return null;
        for (EmployeeRole role : EmployeeRole.values()) {
            if (role.name().equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid employee role: " + value);
    }

    private static CurrentBand parseBand(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) return null;
        for (CurrentBand band : CurrentBand.values()) {
            if (band.name().equalsIgnoreCase(normalized)) {
                return band;
            }
        }
        throw new IllegalArgumentException("Invalid employee band: " + value);
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public record EmployeeResponse(
            String employeeId,
            String employeeName,
            String email,
            String empRole,
            String designation,
            String stream,
            String band,
            String managerId,
            String updatedById,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {}

    public record AddEmployeeWithManagerRequest(
            String employeeId,
            String employeeName,
            String email,
            String empRole,
            String stream,
            String band,
            String managerId,
            String designation,
            String password
    ) {}

    public record UpdateEmployeeRequest(
            String employeeId,
            String employeeName,
            String email,
            String empRole,
            String stream,
            String band,
            String managerId,
            String updatedById,
            String designation,
            String password
    ) {}

    public record CursorPageResponse<T>(
            List<T> items,
            String nextCursor,
            Long total,
            Long managerCount,
            Long adminCount,
            Long employeeCount,
            Long bandCount
    ) {}
}
