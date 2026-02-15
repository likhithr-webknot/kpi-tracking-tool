package com.webknot.kpi.security;

import com.webknot.kpi.models.Employee;
import com.webknot.kpi.repository.EmployeeRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final EmployeeRepository employeeRepository;

    public JwtAuthenticationFilter(JwtService jwtService,
                                   TokenBlacklistService tokenBlacklistService,
                                   EmployeeRepository employeeRepository) {
        this.jwtService = jwtService;
        this.tokenBlacklistService = tokenBlacklistService;
        this.employeeRepository = employeeRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        if (tokenBlacklistService.isRevoked(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email;
        try {
            email = jwtService.extractEmail(token);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Employee employee = employeeRepository.findByEmail(email).orElse(null);
            if (employee != null && jwtService.isTokenValid(token, employee.getEmail())) {
                String role = employee.getEmpRole() != null ? employee.getEmpRole().name() : "Employee";
                UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        employee.getEmail(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}
