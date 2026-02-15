package com.webknot.kpi.service;

import com.webknot.kpi.models.Employee;
import com.webknot.kpi.models.EmployeeRole;
import com.webknot.kpi.repository.EmployeeRepository;
import com.webknot.kpi.security.JwtService;
import com.webknot.kpi.security.TokenBlacklistService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(EmployeeRepository employeeRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       TokenBlacklistService tokenBlacklistService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    public AuthResult login(String email, String password) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(password, employee.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String role = employee.getEmpRole() != null ? employee.getEmpRole().name() : EmployeeRole.Employee.name();
        String portal = switch (employee.getEmpRole() != null ? employee.getEmpRole() : EmployeeRole.Employee) {
            case Admin -> "/portal/admin";
            case Manager -> "/portal/manager";
            case Employee -> "/portal/employee";
        };

        return new AuthResult(jwtService.generateToken(employee), role, portal);
    }

    public void logout(String token) {
        tokenBlacklistService.revokeToken(token, jwtService.extractExpiration(token));
    }

    public record AuthResult(String accessToken, String role, String portal) {}
}
