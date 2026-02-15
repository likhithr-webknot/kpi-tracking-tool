package com.webknot.kpi.service;

import com.webknot.kpi.exceptions.CrudOperationException;
import com.webknot.kpi.exceptions.CrudValidationErrorCode;
import com.webknot.kpi.exceptions.CrudValidationException;
import com.webknot.kpi.models.DesignationLookup;
import com.webknot.kpi.models.Employee;
import com.webknot.kpi.repository.DesignationLookupRepository;
import com.webknot.kpi.repository.EmployeeRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DesignationLookupRepository designationLookupRepository;
    private final Validator validator;
    private final PasswordEncoder passwordEncoder;

    private final Logger logger = Logger.getLogger(EmployeeService.class.getName());

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

    @Transactional(readOnly = true)
    public List<Employee> getAllEmployees() {
        try {
            return employeeRepository.findAll();
        } catch (Exception e) {
            throw CrudOperationException.asFailedGetOperation(Employee.class, e);
        }
    }

    @Transactional(readOnly = true)
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

    @Transactional
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

            var designationId = new DesignationLookup.DesignationId(employee.getStream(), employee.getBand());
            if (!designationLookupRepository.existsById(designationId)) {
                throw new CrudValidationException(Employee.class,
                        "No designation configured for stream=" + employee.getStream() + " and band=" + employee.getBand(),
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

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
            logger.info("Employee created: " + saved.getEmployeeId());
            return saved;
        } catch (CrudValidationException e) {
            throw e;
        } catch (Exception e) {
            throw CrudOperationException.asFailedAddOperation(Employee.class, e);
        }
    }

    @Transactional
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
            var designationId = new DesignationLookup.DesignationId(employee.getStream(), nextBand);
            if (!designationLookupRepository.existsById(designationId)) {
                throw new CrudValidationException(Employee.class,
                        "No designation configured for stream=" + employee.getStream() + " and band=" + nextBand,
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            employee.setBand(nextBand);
            employee.setUpdatedAt(LocalDateTime.now());
            employee.setUpdatedBy(getActor().orElse(employee));

            Employee saved = employeeRepository.save(employee);
            logger.info("Employee promoted: " + saved.getEmployeeId() + " to " + saved.getBand());
            return Optional.of(saved);
        } catch (CrudValidationException e) {
            throw e;
        } catch (Exception e) {
            throw CrudOperationException.asFailedUpdateOperation(Employee.class, e);
        }
    }
}
