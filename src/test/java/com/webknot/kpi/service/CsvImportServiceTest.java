package com.webknot.kpi.service;

import com.webknot.kpi.models.Employee;
import com.webknot.kpi.repository.BandDirectoryRepository;
import com.webknot.kpi.repository.CertificationRepository;
import com.webknot.kpi.repository.DesignationLookupRepository;
import com.webknot.kpi.repository.EmployeeRepository;
import com.webknot.kpi.repository.KpiDefinitionRepository;
import com.webknot.kpi.repository.StreamDirectoryRepository;
import com.webknot.kpi.repository.WebknotValueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock
    private EmployeeService employeeService;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private BandDirectoryService bandDirectoryService;
    @Mock
    private BandDirectoryRepository bandDirectoryRepository;
    @Mock
    private StreamDirectoryService streamDirectoryService;
    @Mock
    private StreamDirectoryRepository streamDirectoryRepository;
    @Mock
    private WebknotValueService webknotValueService;
    @Mock
    private WebknotValueRepository webknotValueRepository;
    @Mock
    private KpiDefinitionService kpiDefinitionService;
    @Mock
    private KpiDefinitionRepository kpiDefinitionRepository;
    @Mock
    private CertificationService certificationService;
    @Mock
    private CertificationRepository certificationRepository;
    @Mock
    private DesignationLookupRepository designationLookupRepository;

    private CsvImportService csvImportService;

    @BeforeEach
    void setUp() {
        csvImportService = new CsvImportService(
                employeeService,
                employeeRepository,
                bandDirectoryService,
                bandDirectoryRepository,
                streamDirectoryService,
                streamDirectoryRepository,
                webknotValueService,
                webknotValueRepository,
                kpiDefinitionService,
                kpiDefinitionRepository,
                certificationService,
                certificationRepository,
                designationLookupRepository,
                "Password@123",
                10000
        );
    }

    @Test
    void importEmployees_rejectsMissingRequiredHeader() {
        MockMultipartFile file = csvFile("""
                employeeId,stream,band
                E001,Development,B7L
                """);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> csvImportService.importEmployees(file)
        );

        assertTrue(ex.getMessage().contains("Missing required columns: email"));
    }

    @Test
    void importEmployees_rejectsExistingEmployeeWhenConfigured() {
        when(employeeRepository.findById("E001")).thenReturn(Optional.of(new Employee()));

        MockMultipartFile file = csvFile("""
                employeeId,email,stream,band
                E001,e001@example.com,Development,B7L
                """);

        CsvImportService.ImportSummary summary = csvImportService.importEmployees(
                file,
                new CsvImportService.ImportOptions(false, true)
        );

        assertEquals(1, summary.totalRows());
        assertEquals(1, summary.failed());
        assertEquals(0, summary.created());
        assertEquals(0, summary.updated());
        assertTrue(summary.errors().get(0).message().contains("Employee already exists: E001"));
        verify(employeeService, never()).updateEmployee(anyString(), any());
        verify(employeeService, never()).addEmployeeWithManager(any(Employee.class), anyString(), anyString());
    }

    @Test
    void importEmployees_validateOnly_doesNotWriteToDatabase() {
        when(employeeRepository.findById("E001")).thenReturn(Optional.of(new Employee()));

        MockMultipartFile file = csvFile("""
                employeeId,email,stream,band,designation
                E001,e001@example.com,Development,B7L,Software Engineer
                """);

        CsvImportService.ImportSummary summary = csvImportService.importEmployees(
                file,
                new CsvImportService.ImportOptions(true, false)
        );

        assertEquals(1, summary.totalRows());
        assertEquals(0, summary.failed());
        assertEquals(0, summary.created());
        assertEquals(1, summary.updated());
        verify(employeeService, never()).updateEmployee(anyString(), any());
        verify(employeeService, never()).addEmployeeWithManager(any(Employee.class), anyString(), anyString());
        verify(designationLookupRepository, never()).save(any());
    }

    @Test
    void importEmployees_detectsDuplicateEmployeeIdsInsideSameFile() {
        when(employeeRepository.findById(anyString())).thenReturn(Optional.empty());

        MockMultipartFile file = csvFile("""
                employeeId,email,stream,band
                E001,e001@example.com,Development,B7L
                E001,e001-dup@example.com,Development,B7L
                """);

        CsvImportService.ImportSummary summary = csvImportService.importEmployees(
                file,
                new CsvImportService.ImportOptions(true, false)
        );

        assertEquals(2, summary.totalRows());
        assertEquals(1, summary.created());
        assertEquals(0, summary.updated());
        assertEquals(1, summary.failed());
        assertTrue(summary.errors().get(0).message().contains("Duplicate employeeId in file: E001"));
    }

    @Test
    void importEmployees_rejectsDuplicateHeaderNames() {
        MockMultipartFile file = csvFile("""
                employeeId,email,email,stream,band
                E001,e001@example.com,e001@example.com,Development,B7L
                """);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> csvImportService.importEmployees(file)
        );

        assertTrue(ex.getMessage().contains("Duplicate column names found in CSV header"));
    }

    @Test
    void importEmployees_rejectsWhenRowLimitExceeded() {
        CsvImportService lowLimitService = new CsvImportService(
                employeeService,
                employeeRepository,
                bandDirectoryService,
                bandDirectoryRepository,
                streamDirectoryService,
                streamDirectoryRepository,
                webknotValueService,
                webknotValueRepository,
                kpiDefinitionService,
                kpiDefinitionRepository,
                certificationService,
                certificationRepository,
                designationLookupRepository,
                "Password@123",
                1
        );

        MockMultipartFile file = csvFile("""
                employeeId,email,stream,band
                E001,e001@example.com,Development,B7L
                E002,e002@example.com,Development,B7L
                """);

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> lowLimitService.importEmployees(file)
        );

        assertTrue(ex.getMessage().contains("CSV row limit exceeded"));
    }

    private static MockMultipartFile csvFile(String content) {
        return new MockMultipartFile(
                "file",
                "import.csv",
                "text/csv",
                content.getBytes(StandardCharsets.UTF_8)
        );
    }
}
