package com.webknot.kpi.service;

import com.webknot.kpi.exceptions.CrudOperationException;
import com.webknot.kpi.exceptions.CrudValidationErrorCode;
import com.webknot.kpi.exceptions.CrudValidationException;
import com.webknot.kpi.models.Employee;
import com.webknot.kpi.repository.EmployeeRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final Validator validator;

    private final Logger logger = Logger.getLogger(EmployeeService.class.getName());

    public EmployeeService(EmployeeRepository employeeRepository, Validator validator) {
        this.employeeRepository = employeeRepository;
        this.validator = validator;
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

            Employee saved = employeeRepository.save(employee);
            logger.info("Employee created: " + saved.getEmployeeId());
            return saved;
        } catch (CrudValidationException e) {
            throw e;
        } catch (Exception e) {
            throw CrudOperationException.asFailedAddOperation(Employee.class, e);
        }
    }
}
