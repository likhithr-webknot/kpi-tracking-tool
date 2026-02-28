package com.webknot.kpi.service;

import com.webknot.kpi.exceptions.CrudOperationException;
import com.webknot.kpi.exceptions.CrudValidationErrorCode;
import com.webknot.kpi.exceptions.CrudValidationException;
import com.webknot.kpi.models.DesignationLookup;
import com.webknot.kpi.models.Employee;
import com.webknot.kpi.models.EmployeeRole;
import com.webknot.kpi.repository.DesignationLookupRepository;
import com.webknot.kpi.repository.EmployeeRepository;
import com.webknot.kpi.util.BandStreamNormalizer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class EmployeeService {
    private static final int DEFAULT_CURSOR_LIMIT = 10;
    private static final int MAX_CURSOR_LIMIT = 100;

    private final EmployeeRepository employeeRepository;
    private final DesignationLookupRepository designationLookupRepository;
    private final Validator validator;
    private final PasswordEncoder passwordEncoder;

    private static final Logger log = LoggerFactory.getLogger(EmployeeService.class);

    public EmployeeService(EmployeeRepository employeeRepository,
                           DesignationLookupRepository designationLookupRepository,
                           Validator validator,
                           PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.designationLookupRepository = designationLookupRepository;
        this.validator = validator;
        this.passwordEncoder = passwordEncoder;
    }

    private void checkForNull(Employee emp) {
        if (emp == null) {
            throw CrudOperationException.asNullEntity(Employee.class);
        }
    }

    private void validate(Employee emp) throws CrudValidationException {
        Set<ConstraintViolation<Employee>> violations = validator.validate(emp);
        if (!violations.isEmpty()) {
            throw CrudValidationException.asFailedValidationOperation(Employee.class, violations);
        }
    }

    private Optional<Employee> getActor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        String email = auth.getName();
        if (email == null || email.isBlank() || "anonymousUser".equalsIgnoreCase(email)) {
            return Optional.empty();
        }
        return employeeRepository.findByEmail(email);
    }

    public List<Employee> getAllEmployees() {
        try {
            return employeeRepository.findAll();
        } catch (Exception e) {
            throw CrudOperationException.asFailedGetOperation(Employee.class, e);
        }
    }

    public EmployeeCursorPage getEmployeesCursorPage(Integer limit, String cursor) {
        int pageSize = normalizeCursorLimit(limit);
        String startAfter = cursor == null || cursor.isBlank() ? null : cursor.trim();
        Pageable pageable = PageRequest.of(0, pageSize + 1);

        try {
            long total = employeeRepository.count();
            long managerCount = employeeRepository.countByEmpRole(EmployeeRole.Manager);
            long adminCount = employeeRepository.countByEmpRole(EmployeeRole.Admin);
            long employeeCount = employeeRepository.countByEmpRole(EmployeeRole.Employee);
            long bandCount = employeeRepository.countDistinctBand();
            List<Employee> rows = startAfter == null
                    ? employeeRepository.findAllByOrderByEmployeeIdAsc(pageable)
                    : employeeRepository.findByEmployeeIdGreaterThanOrderByEmployeeIdAsc(startAfter, pageable);

            boolean hasMore = rows.size() > pageSize;
            List<Employee> items = hasMore ? rows.subList(0, pageSize) : rows;
            String nextCursor = hasMore && !items.isEmpty()
                    ? items.get(items.size() - 1).getEmployeeId()
                    : null;

            return new EmployeeCursorPage(
                    List.copyOf(items),
                    nextCursor,
                    total,
                    managerCount,
                    adminCount,
                    employeeCount,
                    bandCount
            );
        } catch (Exception e) {
            throw CrudOperationException.asFailedGetOperation(Employee.class, e);
        }
    }

    public Optional<Employee> getEmployeeById(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new CrudValidationException(Employee.class,
                    "Employee ID cannot be null/blank",
                    CrudValidationErrorCode.INVALID_IDENTIFIER);
        }

        try {
            return employeeRepository.findById(employeeId);
        } catch (Exception e) {
            throw CrudOperationException.asFailedGetOperation(Employee.class, e);
        }
    }

    public Employee addEmployee(Employee employee) {
        checkForNull(employee);
        validate(employee);

        try {
            if (employee.getEmployeeId() == null || employee.getEmployeeId().isBlank()) {
                throw new CrudValidationException(Employee.class,
                        "Employee ID cannot be null/blank",
                        CrudValidationErrorCode.INVALID_IDENTIFIER);
            }

            if (employeeRepository.existsById(employee.getEmployeeId())) {
                throw new CrudValidationException(Employee.class,
                        "Employee already exists with ID = " + employee.getEmployeeId(),
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            if (employee.getEmail() == null || employee.getEmail().isBlank()) {
                throw new CrudValidationException(Employee.class,
                        "Email cannot be null/blank",
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            if (employeeRepository.existsByEmail(employee.getEmail())) {
                throw new CrudValidationException(Employee.class,
                        "Employee already exists with email = " + employee.getEmail(),
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            if (employee.getPassword() == null || employee.getPassword().isBlank()) {
                throw new CrudValidationException(Employee.class,
                        "Password cannot be null/blank",
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            if (employee.getBand() == null || employee.getStream() == null || employee.getStream().isBlank()) {
                throw new CrudValidationException(Employee.class,
                        "Employee stream and band are required",
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            String designationStream = resolveDesignationLookupStream(employee.getStream(), employee.getBand());
            var designationId = new DesignationLookup.DesignationId(designationStream, employee.getBand());
            if (!designationLookupRepository.existsById(designationId)) {
                throw new CrudValidationException(Employee.class,
                        "No designation configured for stream=" + designationStream + " and band=" + employee.getBand(),
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            employee.setStream(designationStream);
            employee.setPassword(passwordEncoder.encode(employee.getPassword()));
            if (employee.getCreatedAt() == null) {
                employee.setCreatedAt(LocalDateTime.now());
            }
            if (employee.getUpdatedAt() == null) {
                employee.setUpdatedAt(LocalDateTime.now());
            }

            Employee saved = employeeRepository.save(employee);
            if (saved.getUpdatedBy() == null) {
                Employee updater = getActor().orElse(saved);
                saved.setUpdatedBy(updater);
                saved = employeeRepository.save(saved);
            }
            log.info("Employee created id={} stream={} band={}", saved.getEmployeeId(), saved.getStream(), saved.getBand());
            return saved;
        } catch (CrudValidationException e) {
            throw e;
        } catch (Exception e) {
            throw CrudOperationException.asFailedAddOperation(Employee.class, e);
        }
    }

    public Employee addEmployeeWithManager(Employee employee, String managerId, String defaultPassword) {
        if (employee == null) {
            throw CrudOperationException.asNullEntity(Employee.class);
        }

        if (employee.getEmpRole() == null) {
            employee.setEmpRole(EmployeeRole.Employee);
        }

        if (employee.getPassword() == null || employee.getPassword().isBlank()) {
            employee.setPassword(defaultPassword);
        }

        if (managerId != null && !managerId.isBlank()) {
            Employee manager = employeeRepository.findById(managerId)
                    .orElseThrow(() -> new CrudValidationException(
                            Employee.class,
                            "Manager not found: " + managerId,
                            CrudValidationErrorCode.INVALID_IDENTIFIER
                    ));
            employee.setManager(manager);
        }

        return addEmployee(employee);
    }

    public List<Employee> getManagers() {
        try {
            return employeeRepository.findByEmpRole(EmployeeRole.Manager);
        } catch (Exception e) {
            throw CrudOperationException.asFailedGetOperation(Employee.class, e);
        }
    }

    public List<Employee> getReporteesByManagerId(String managerId) {
        if (managerId == null || managerId.isBlank()) {
            throw new CrudValidationException(Employee.class,
                    "Manager ID cannot be null/blank",
                    CrudValidationErrorCode.INVALID_IDENTIFIER);
        }

        try {
            return employeeRepository.findByManager_EmployeeId(managerId);
        } catch (Exception e) {
            throw CrudOperationException.asFailedGetOperation(Employee.class, e);
        }
    }

    public Optional<Employee> promoteEmployee(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new CrudValidationException(Employee.class,
                    "Employee ID cannot be null/blank",
                    CrudValidationErrorCode.INVALID_IDENTIFIER);
        }

        try {
            Optional<Employee> existingOpt = employeeRepository.findById(employeeId);
            if (existingOpt.isEmpty()) {
                return Optional.empty();
            }

            Employee employee = existingOpt.get();
            if (employee.getBand() == null) {
                throw new CrudValidationException(Employee.class,
                        "Employee band cannot be null",
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            var nextBandOpt = employee.getBand().oneBandAbove();
            if (nextBandOpt.isEmpty()) {
                throw new CrudValidationException(Employee.class,
                        "Employee is already at the highest band: " + employee.getBand(),
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            var nextBand = nextBandOpt.get();
            String designationStream = resolveDesignationLookupStream(employee.getStream(), nextBand);
            var designationId = new DesignationLookup.DesignationId(designationStream, nextBand);
            if (!designationLookupRepository.existsById(designationId)) {
                throw new CrudValidationException(Employee.class,
                        "No designation configured for stream=" + designationStream + " and band=" + nextBand,
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            employee.setStream(designationStream);
            employee.setBand(nextBand);
            employee.setUpdatedAt(LocalDateTime.now());
            employee.setUpdatedBy(getActor().orElse(employee));

            Employee saved = employeeRepository.save(employee);
            log.info("Employee promoted id={} toBand={} stream={}", saved.getEmployeeId(), saved.getBand(), saved.getStream());
            return Optional.of(saved);
        } catch (CrudValidationException e) {
            throw e;
        } catch (Exception e) {
            throw CrudOperationException.asFailedUpdateOperation(Employee.class, e);
        }
    }

    public Optional<Employee> updateEmployee(String employeeId, EmployeeUpdateCommand command) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new CrudValidationException(Employee.class,
                    "Employee ID cannot be null/blank",
                    CrudValidationErrorCode.INVALID_IDENTIFIER);
        }
        if (command == null) {
            throw CrudOperationException.asNullEntity(Employee.class);
        }

        try {
            Optional<Employee> existingOpt = employeeRepository.findById(employeeId);
            if (existingOpt.isEmpty()) {
                return Optional.empty();
            }

            Employee employee = existingOpt.get();
            String payloadEmployeeId = trimToNull(command.employeeId());
            if (payloadEmployeeId != null && !employeeId.equals(payloadEmployeeId)) {
                throw new CrudValidationException(Employee.class,
                        "Path id and payload employeeId mismatch.",
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            String nextEmail = trimToNull(command.email());
            if (nextEmail == null) {
                nextEmail = trimToNull(employee.getEmail());
            }
            if (nextEmail == null) {
                throw new CrudValidationException(Employee.class,
                        "Email cannot be null/blank",
                        CrudValidationErrorCode.DATA_VALIDATION);
            }
            if (!nextEmail.equalsIgnoreCase(String.valueOf(employee.getEmail()))
                    && employeeRepository.existsByEmail(nextEmail)) {
                throw new CrudValidationException(Employee.class,
                        "Employee already exists with email = " + nextEmail,
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            EmployeeRole nextRole = command.empRole() != null ? command.empRole() : employee.getEmpRole();
            if (nextRole == null) {
                nextRole = EmployeeRole.Employee;
            }

            com.webknot.kpi.models.CurrentBand nextBand = command.band() != null ? command.band() : employee.getBand();
            String requestedStream = trimToNull(command.stream());
            String nextStream = requestedStream != null ? requestedStream : trimToNull(employee.getStream());
            if (nextBand == null || nextStream == null) {
                throw new CrudValidationException(Employee.class,
                        "Employee stream and band are required",
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            String designationStream = resolveDesignationLookupStream(nextStream, nextBand);
            if (designationStream == null || designationStream.isBlank()) {
                throw new CrudValidationException(Employee.class,
                        "No designation configured for stream=" + nextStream + " and band=" + nextBand,
                        CrudValidationErrorCode.DATA_VALIDATION);
            }
            DesignationLookup.DesignationId designationId = new DesignationLookup.DesignationId(designationStream, nextBand);
            if (!designationLookupRepository.existsById(designationId)) {
                throw new CrudValidationException(Employee.class,
                        "No designation configured for stream=" + designationStream + " and band=" + nextBand,
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            Employee manager = employee.getManager();
            if (command.managerId() != null) {
                String managerId = trimToNull(command.managerId());
                if (managerId == null) {
                    manager = null;
                } else {
                    if (employeeId.equals(managerId)) {
                        throw new CrudValidationException(Employee.class,
                                "Employee cannot report to itself",
                                CrudValidationErrorCode.DATA_VALIDATION);
                    }
                    manager = employeeRepository.findById(managerId)
                            .orElseThrow(() -> new CrudValidationException(
                                    Employee.class,
                                    "Manager not found: " + managerId,
                                    CrudValidationErrorCode.INVALID_IDENTIFIER
                            ));
                }
            }

            Employee updatedBy = getActor().orElse(employee);
            String updatedById = trimToNull(command.updatedById());
            if (updatedById != null) {
                updatedBy = employeeRepository.findById(updatedById)
                        .orElseThrow(() -> new CrudValidationException(
                                Employee.class,
                                "UpdatedBy employee not found: " + updatedById,
                                CrudValidationErrorCode.INVALID_IDENTIFIER
                        ));
            }

            String nextName = trimToNull(command.employeeName());
            if (nextName != null) {
                employee.setEmployeeName(nextName);
            }
            employee.setEmail(nextEmail);
            employee.setEmpRole(nextRole);
            employee.setBand(nextBand);
            employee.setStream(designationStream);
            employee.setManager(manager);
            employee.setUpdatedBy(updatedBy);
            employee.setUpdatedAt(LocalDateTime.now());

            String nextPassword = trimToNull(command.password());
            if (nextPassword != null) {
                employee.setPassword(passwordEncoder.encode(nextPassword));
            }

            Employee saved = employeeRepository.save(employee);
            log.info("Employee updated id={} role={} band={} stream={} managerId={}",
                    saved.getEmployeeId(),
                    saved.getEmpRole(),
                    saved.getBand(),
                    saved.getStream(),
                    saved.getManager() != null ? saved.getManager().getEmployeeId() : null);
            return Optional.of(saved);
        } catch (CrudValidationException e) {
            throw e;
        } catch (Exception e) {
            throw CrudOperationException.asFailedUpdateOperation(Employee.class, e);
        }
    }

    public boolean deleteEmployee(String employeeId) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new CrudValidationException(Employee.class,
                    "Employee ID cannot be null/blank",
                    CrudValidationErrorCode.INVALID_IDENTIFIER);
        }

        try {
            Optional<Employee> actor = getActor();
            if (actor.isPresent() && employeeId.equals(actor.get().getEmployeeId())) {
                throw new CrudValidationException(Employee.class,
                        "You cannot delete your own user.",
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            Optional<Employee> existingOpt = employeeRepository.findById(employeeId);
            if (existingOpt.isEmpty()) {
                return false;
            }

            employeeRepository.delete(existingOpt.get());
            log.info("Employee deleted id={}", employeeId);
            return true;
        } catch (CrudValidationException e) {
            throw e;
        } catch (Exception e) {
            throw CrudOperationException.asFailedDeleteOperation(Employee.class, e);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private int normalizeCursorLimit(Integer limit) {
        if (limit == null || limit <= 0) return DEFAULT_CURSOR_LIMIT;
        return Math.min(limit, MAX_CURSOR_LIMIT);
    }

    private String resolveDesignationLookupStream(String rawStream, com.webknot.kpi.models.CurrentBand band) {
        String canonical = BandStreamNormalizer.canonicalStreamLabel(rawStream);
        if (canonical != null && band != null) {
            DesignationLookup.DesignationId canonicalId = new DesignationLookup.DesignationId(canonical, band);
            if (designationLookupRepository.existsById(canonicalId)) {
                return canonical;
            }
        }
        String raw = rawStream == null ? null : rawStream.trim();
        if (raw != null && !raw.isBlank() && band != null) {
            DesignationLookup.DesignationId rawId = new DesignationLookup.DesignationId(raw, band);
            if (designationLookupRepository.existsById(rawId)) {
                return raw;
            }
        }
        return canonical;
    }

    public record EmployeeCursorPage(
            List<Employee> items,
            String nextCursor,
            Long total,
            Long managerCount,
            Long adminCount,
            Long employeeCount,
            Long bandCount
    ) {}

    public record EmployeeUpdateCommand(
            String employeeId,
            String employeeName,
            String email,
            EmployeeRole empRole,
            String stream,
            com.webknot.kpi.models.CurrentBand band,
            String managerId,
            String updatedById,
            String password
    ) {}
}
