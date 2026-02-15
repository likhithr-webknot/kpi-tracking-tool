package com.webknot.kpi.controller;

import com.webknot.kpi.exceptions.CrudValidationErrorCode;
import com.webknot.kpi.exceptions.CrudValidationException;
import com.webknot.kpi.models.CurrentBand;
import com.webknot.kpi.models.Employee;
import com.webknot.kpi.models.EmployeeRole;
import com.webknot.kpi.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerPromotionTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeService employeeService;

    @Test
    void promoteEmployee_returnsUpdatedEmployee() throws Exception {
        Employee promoted = new Employee();
        promoted.setEmployeeId("E100");
        promoted.setEmployeeName("Alex");
        promoted.setEmail("alex@example.com");
        promoted.setEmpRole(EmployeeRole.Employee);
        promoted.setStream("Engineering");
        promoted.setBand(CurrentBand.B7L);

        when(employeeService.promoteEmployee("E100")).thenReturn(Optional.of(promoted));

        mockMvc.perform(post("/employees/E100/promote")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeId").value("E100"))
                .andExpect(jsonPath("$.band").value("B7L"));
    }

    @Test
    void promoteEmployee_returns404WhenEmployeeMissing() throws Exception {
        when(employeeService.promoteEmployee("MISSING")).thenReturn(Optional.empty());

        mockMvc.perform(post("/employees/MISSING/promote")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Employee not found: MISSING"));
    }

    @Test
    void promoteEmployee_returns400OnValidationError() throws Exception {
        when(employeeService.promoteEmployee("E200")).thenThrow(
                new CrudValidationException(Employee.class,
                        "Employee is already at the highest band: B4",
                        CrudValidationErrorCode.DATA_VALIDATION)
        );

        mockMvc.perform(post("/employees/E200/promote")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Employee is already at the highest band: B4"));
    }
}
