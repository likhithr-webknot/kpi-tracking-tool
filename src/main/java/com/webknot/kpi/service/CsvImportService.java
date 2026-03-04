package com.webknot.kpi.service;

import com.webknot.kpi.models.BandDirectory;
import com.webknot.kpi.models.Certification;
import com.webknot.kpi.models.CurrentBand;
import com.webknot.kpi.models.CurrentStream;
import com.webknot.kpi.models.DesignationLookup;
import com.webknot.kpi.models.Employee;
import com.webknot.kpi.models.EmployeeRole;
import com.webknot.kpi.models.KpiDefinition;
import com.webknot.kpi.models.StreamDirectory;
import com.webknot.kpi.models.WebknotValue;
import com.webknot.kpi.repository.BandDirectoryRepository;
import com.webknot.kpi.repository.CertificationRepository;
import com.webknot.kpi.repository.DesignationLookupRepository;
import com.webknot.kpi.repository.EmployeeRepository;
import com.webknot.kpi.repository.KpiDefinitionRepository;
import com.webknot.kpi.repository.StreamDirectoryRepository;
import com.webknot.kpi.repository.WebknotValueRepository;
import com.webknot.kpi.util.BandStreamNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class CsvImportService {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]");
    private static final int MAX_ERROR_ROWS = 200;

    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;
    private final BandDirectoryService bandDirectoryService;
    private final BandDirectoryRepository bandDirectoryRepository;
    private final StreamDirectoryService streamDirectoryService;
    private final StreamDirectoryRepository streamDirectoryRepository;
    private final WebknotValueService webknotValueService;
    private final WebknotValueRepository webknotValueRepository;
    private final KpiDefinitionService kpiDefinitionService;
    private final KpiDefinitionRepository kpiDefinitionRepository;
    private final CertificationService certificationService;
    private final CertificationRepository certificationRepository;
    private final DesignationLookupRepository designationLookupRepository;
    private final String defaultEmployeePassword;
    private final int maxCsvRows;

    public CsvImportService(EmployeeService employeeService,
                            EmployeeRepository employeeRepository,
                            BandDirectoryService bandDirectoryService,
                            BandDirectoryRepository bandDirectoryRepository,
                            StreamDirectoryService streamDirectoryService,
                            StreamDirectoryRepository streamDirectoryRepository,
                            WebknotValueService webknotValueService,
                            WebknotValueRepository webknotValueRepository,
                            KpiDefinitionService kpiDefinitionService,
                            KpiDefinitionRepository kpiDefinitionRepository,
                            CertificationService certificationService,
                            CertificationRepository certificationRepository,
                            DesignationLookupRepository designationLookupRepository,
                            @Value("${employee.default-password:Password@123}") String defaultEmployeePassword,
                            @Value("${imports.csv.max-rows:10000}") int maxCsvRows) {
        this.employeeService = employeeService;
        this.employeeRepository = employeeRepository;
        this.bandDirectoryService = bandDirectoryService;
        this.bandDirectoryRepository = bandDirectoryRepository;
        this.streamDirectoryService = streamDirectoryService;
        this.streamDirectoryRepository = streamDirectoryRepository;
        this.webknotValueService = webknotValueService;
        this.webknotValueRepository = webknotValueRepository;
        this.kpiDefinitionService = kpiDefinitionService;
        this.kpiDefinitionRepository = kpiDefinitionRepository;
        this.certificationService = certificationService;
        this.certificationRepository = certificationRepository;
        this.designationLookupRepository = designationLookupRepository;
        this.defaultEmployeePassword = defaultEmployeePassword;
        this.maxCsvRows = Math.max(1, maxCsvRows);
    }

    public ImportSummary importEmployees(MultipartFile file) {
        return importEmployees(file, ImportOptions.defaults());
    }

    public ImportSummary importEmployees(MultipartFile file, ImportOptions options) {
        ImportOptions resolvedOptions = resolveOptions(options);
        CsvData csv = parseCsv(file);
        validateRequiredColumns(
                csv,
                List.of("employeeid", "employeecode", "id"),
                List.of("email", "mail"),
                List.of("stream", "streamcode", "currentstream"),
                List.of("band", "currentband")
        );
        int created = 0;
        int updated = 0;
        Map<Integer, String> errorsByRow = new LinkedHashMap<>();
        List<EmployeeRelationUpdate> deferredRelationUpdates = new ArrayList<>();
        Set<String> employeeIdsInSheet = new HashSet<>();

        for (CsvRow row : csv.rows()) {
            try {
                String employeeId = require(row, "employeeid", "employeecode", "id");
                String employeeKey = normalizeKey(employeeId);
                if (!employeeIdsInSheet.add(employeeKey)) {
                    throw new IllegalArgumentException("Duplicate employeeId in file: " + employeeId);
                }
                String email = require(row, "email", "mail");
                String stream = require(row, "stream", "streamcode", "currentstream");
                CurrentBand band = parseBand(require(row, "band", "currentband"));
                String designation = value(row, "designation", "title");

                String password = firstNonBlank(value(row, "password", "passwd"), defaultEmployeePassword);
                EmployeeRole role = parseRoleNullable(value(row, "emprole", "role", "employeerole"));

                Optional<Employee> existing = employeeRepository.findById(employeeId);
                if (existing.isPresent()) {
                    if (resolvedOptions.rejectExisting()) {
                        throw new IllegalArgumentException("Employee already exists: " + employeeId);
                    }
                    if (resolvedOptions.validateOnly()) {
                        updated++;
                    } else {
                        ensureDesignationLookupExists(stream, band, designation);
                        EmployeeService.EmployeeUpdateCommand command = new EmployeeService.EmployeeUpdateCommand(
                                null,
                                value(row, "employeename", "name"),
                                email,
                                role,
                                stream,
                                band,
                                null,
                                null,
                                value(row, "password", "passwd")
                        );
                        employeeService.updateEmployee(employeeId, command)
                                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
                        updated++;
                    }
                } else {
                    if (resolvedOptions.validateOnly()) {
                        created++;
                    } else {
                        ensureDesignationLookupExists(stream, band, designation);
                        Employee employee = new Employee();
                        employee.setEmployeeId(employeeId);
                        employee.setEmployeeName(value(row, "employeename", "name"));
                        employee.setEmail(email);
                        employee.setEmpRole(role != null ? role : EmployeeRole.Employee);
                        employee.setStream(stream);
                        employee.setBand(band);
                        employee.setPassword(password);
                        employeeService.addEmployeeWithManager(employee, null, defaultEmployeePassword);
                        created++;
                    }
                }

                boolean managerColumnPresent = hasAnyHeader(row, "managerid", "manager");
                boolean updatedByColumnPresent = hasAnyHeader(row, "updatedbyid", "updatedby");
                if (managerColumnPresent || updatedByColumnPresent) {
                    deferredRelationUpdates.add(new EmployeeRelationUpdate(
                            row.rowNumber(),
                            employeeId,
                            managerColumnPresent,
                            rawValue(row, "managerid", "manager"),
                            updatedByColumnPresent,
                            rawValue(row, "updatedbyid", "updatedby")
                    ));
                }
            } catch (Exception e) {
                registerError(errorsByRow, row.rowNumber(), e.getMessage());
            }
        }

        if (!resolvedOptions.validateOnly()) {
            for (EmployeeRelationUpdate update : deferredRelationUpdates) {
                if (errorsByRow.containsKey(update.rowNumber())) {
                    continue;
                }
                try {
                    EmployeeService.EmployeeUpdateCommand relationCommand = new EmployeeService.EmployeeUpdateCommand(
                            null,
                            null,
                            null,
                            null,
                            null,
                            null,
                            update.managerColumnPresent() ? update.managerRaw() : null,
                            update.updatedByColumnPresent() ? update.updatedByRaw() : null,
                            null
                    );
                    employeeService.updateEmployee(update.employeeId(), relationCommand)
                            .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + update.employeeId()));
                } catch (Exception e) {
                    registerError(errorsByRow, update.rowNumber(), e.getMessage());
                }
            }
        }

        return buildSummary("employees", csv.rows().size(), created, updated, errorsByRow);
    }

    public ImportSummary importBands(MultipartFile file) {
        return importBands(file, ImportOptions.defaults());
    }

    public ImportSummary importBands(MultipartFile file, ImportOptions options) {
        ImportOptions resolvedOptions = resolveOptions(options);
        CsvData csv = parseCsv(file);
        validateRequiredColumns(csv, List.of("code", "band", "bandcode"));
        int created = 0;
        int updated = 0;
        Map<Integer, String> errorsByRow = new LinkedHashMap<>();
        Set<String> bandCodesInSheet = new HashSet<>();

        for (CsvRow row : csv.rows()) {
            try {
                String code = require(row, "code", "band", "bandcode");
                String bandKey = normalizeKey(code);
                if (!bandCodesInSheet.add(bandKey)) {
                    throw new IllegalArgumentException("Duplicate band code in file: " + code);
                }
                String label = value(row, "label", "name", "title");
                Boolean active = parseBooleanNullable(value(row, "active", "isactive"));
                Integer sortOrder = parseIntegerNullable(value(row, "sortorder", "sort", "order"));

                CurrentBand parsedCode = parseBand(code);
                if (bandDirectoryRepository.existsById(parsedCode)) {
                    if (resolvedOptions.rejectExisting()) {
                        throw new IllegalArgumentException("Band already exists: " + parsedCode.name());
                    }
                    if (resolvedOptions.validateOnly()) {
                        updated++;
                    } else {
                        bandDirectoryService.update(parsedCode.name(), label, active, sortOrder);
                        updated++;
                    }
                } else {
                    if (resolvedOptions.validateOnly()) {
                        created++;
                    } else {
                        bandDirectoryService.add(parsedCode.name(), label, active, sortOrder);
                        created++;
                    }
                }
            } catch (Exception e) {
                registerError(errorsByRow, row.rowNumber(), e.getMessage());
            }
        }

        return buildSummary("bands", csv.rows().size(), created, updated, errorsByRow);
    }

    public ImportSummary importStreams(MultipartFile file) {
        return importStreams(file, ImportOptions.defaults());
    }

    public ImportSummary importStreams(MultipartFile file, ImportOptions options) {
        ImportOptions resolvedOptions = resolveOptions(options);
        CsvData csv = parseCsv(file);
        validateRequiredColumns(csv, List.of("code", "stream", "streamcode"));
        int created = 0;
        int updated = 0;
        Map<Integer, String> errorsByRow = new LinkedHashMap<>();
        Set<String> streamCodesInSheet = new HashSet<>();

        for (CsvRow row : csv.rows()) {
            try {
                String code = require(row, "code", "stream", "streamcode");
                String streamKey = normalizeKey(code);
                if (!streamCodesInSheet.add(streamKey)) {
                    throw new IllegalArgumentException("Duplicate stream code in file: " + code);
                }
                String label = value(row, "label", "name", "title");
                Boolean active = parseBooleanNullable(value(row, "active", "isactive"));
                Integer sortOrder = parseIntegerNullable(value(row, "sortorder", "sort", "order"));

                CurrentStream parsedCode = parseStream(code);
                if (streamDirectoryRepository.existsById(parsedCode)) {
                    if (resolvedOptions.rejectExisting()) {
                        throw new IllegalArgumentException("Stream already exists: " + parsedCode.name());
                    }
                    if (resolvedOptions.validateOnly()) {
                        updated++;
                    } else {
                        streamDirectoryService.update(parsedCode.name(), label, active, sortOrder);
                        updated++;
                    }
                } else {
                    if (resolvedOptions.validateOnly()) {
                        created++;
                    } else {
                        streamDirectoryService.add(parsedCode.name(), label, active, sortOrder);
                        created++;
                    }
                }
            } catch (Exception e) {
                registerError(errorsByRow, row.rowNumber(), e.getMessage());
            }
        }

        return buildSummary("streams", csv.rows().size(), created, updated, errorsByRow);
    }

    public ImportSummary importWebknotValues(MultipartFile file) {
        return importWebknotValues(file, ImportOptions.defaults());
    }

    public ImportSummary importWebknotValues(MultipartFile file, ImportOptions options) {
        ImportOptions resolvedOptions = resolveOptions(options);
        CsvData csv = parseCsv(file);
        validateRequiredColumns(csv, List.of("title", "value", "name", "label"));
        int created = 0;
        int updated = 0;
        Map<Integer, String> errorsByRow = new LinkedHashMap<>();
        Set<String> titlesInSheet = new HashSet<>();

        for (CsvRow row : csv.rows()) {
            try {
                String title = require(row, "title", "value", "name", "label");
                String titleKey = normalizeKey(title);
                if (!titlesInSheet.add(titleKey)) {
                    throw new IllegalArgumentException("Duplicate webknot value title in file: " + title);
                }
                String pillar = value(row, "pillar", "pillarname");
                String description = value(row, "description", "desc", "details");
                Boolean active = parseBooleanNullable(value(row, "active", "isactive"));

                Optional<WebknotValue> existing = webknotValueRepository.findByTitleIgnoreCase(title);
                if (existing.isPresent()) {
                    if (resolvedOptions.rejectExisting()) {
                        throw new IllegalArgumentException("Webknot value already exists: " + title);
                    }
                    if (resolvedOptions.validateOnly()) {
                        updated++;
                    } else {
                        webknotValueService.update(existing.get().getId(), title, pillar, description, active);
                        updated++;
                    }
                } else {
                    if (resolvedOptions.validateOnly()) {
                        created++;
                    } else {
                        webknotValueService.add(title, pillar, description, active);
                        created++;
                    }
                }
            } catch (Exception e) {
                registerError(errorsByRow, row.rowNumber(), e.getMessage());
            }
        }

        return buildSummary("webknot-values", csv.rows().size(), created, updated, errorsByRow);
    }

    public ImportSummary importKpiDefinitions(MultipartFile file) {
        return importKpiDefinitions(file, ImportOptions.defaults());
    }

    public ImportSummary importKpiDefinitions(MultipartFile file, ImportOptions options) {
        ImportOptions resolvedOptions = resolveOptions(options);
        CsvData csv = parseCsv(file);
        validateRequiredColumns(
                csv,
                List.of("band"),
                List.of("stream"),
                List.of("kpiname", "kpi", "name"),
                List.of("weightage", "weight", "percentage")
        );
        int created = 0;
        int updated = 0;
        Map<Integer, String> errorsByRow = new LinkedHashMap<>();
        Set<String> kpiKeysInSheet = new HashSet<>();

        for (CsvRow row : csv.rows()) {
            try {
                CurrentBand band = parseBand(require(row, "band"));
                CurrentStream stream = parseStream(require(row, "stream"));
                String kpiName = require(row, "kpiname", "kpi", "name");
                String kpiKey = normalizeKey(band.name() + "|" + stream.name() + "|" + kpiName);
                if (!kpiKeysInSheet.add(kpiKey)) {
                    throw new IllegalArgumentException(
                            "Duplicate KPI row in file for band=" + band + ", stream=" + stream + ", kpiName=" + kpiName);
                }
                BigDecimal weightage = parseDecimal(require(row, "weightage", "weight", "percentage"));
                String description = value(row, "description", "desc", "details");

                Optional<KpiDefinition> existing = kpiDefinitionRepository.findByBandAndStreamAndKpiName(band, stream, kpiName);
                if (existing.isPresent()) {
                    if (resolvedOptions.rejectExisting()) {
                        throw new IllegalArgumentException(
                                "KPI definition already exists for band=" + band + ", stream=" + stream + ", kpiName=" + kpiName);
                    }
                    if (resolvedOptions.validateOnly()) {
                        updated++;
                    } else {
                        KpiDefinition candidate = new KpiDefinition();
                        candidate.setId(existing.get().getId());
                        candidate.setBand(band);
                        candidate.setStream(stream);
                        candidate.setKpiName(kpiName);
                        candidate.setWeightage(weightage);
                        candidate.setDescription(firstNonBlank(description, existing.get().getDescription()));
                        kpiDefinitionService.update(candidate);
                        updated++;
                    }
                } else {
                    if (resolvedOptions.validateOnly()) {
                        created++;
                    } else {
                        KpiDefinition candidate = new KpiDefinition();
                        candidate.setBand(band);
                        candidate.setStream(stream);
                        candidate.setKpiName(kpiName);
                        candidate.setWeightage(weightage);
                        candidate.setDescription(description);
                        kpiDefinitionService.add(candidate);
                        created++;
                    }
                }
            } catch (Exception e) {
                registerError(errorsByRow, row.rowNumber(), e.getMessage());
            }
        }

        return buildSummary("kpi-definitions", csv.rows().size(), created, updated, errorsByRow);
    }

    public ImportSummary importCertifications(MultipartFile file) {
        return importCertifications(file, ImportOptions.defaults());
    }

    public ImportSummary importCertifications(MultipartFile file, ImportOptions options) {
        ImportOptions resolvedOptions = resolveOptions(options);
        CsvData csv = parseCsv(file);
        validateRequiredColumns(csv, List.of("name", "certification", "certificationname", "title"));
        int created = 0;
        int updated = 0;
        Map<Integer, String> errorsByRow = new LinkedHashMap<>();
        Set<String> namesInSheet = new HashSet<>();

        for (CsvRow row : csv.rows()) {
            try {
                String name = require(row, "name", "certification", "certificationname", "title");
                String nameKey = normalizeKey(name);
                if (!namesInSheet.add(nameKey)) {
                    throw new IllegalArgumentException("Duplicate certification name in file: " + name);
                }
                Boolean active = parseBooleanNullable(value(row, "active", "isactive"));

                Optional<Certification> existing = certificationRepository.findByNameIgnoreCase(name);
                if (existing.isPresent()) {
                    if (resolvedOptions.rejectExisting()) {
                        throw new IllegalArgumentException("Certification already exists: " + name);
                    }
                    if (resolvedOptions.validateOnly()) {
                        updated++;
                    } else {
                        certificationService.update(existing.get().getId(), name, active);
                        updated++;
                    }
                } else {
                    if (resolvedOptions.validateOnly()) {
                        created++;
                    } else {
                        certificationService.add(name, active);
                        created++;
                    }
                }
            } catch (Exception e) {
                registerError(errorsByRow, row.rowNumber(), e.getMessage());
            }
        }

        return buildSummary("certifications", csv.rows().size(), created, updated, errorsByRow);
    }

    public ImportSummary importDesignationLookups(MultipartFile file) {
        return importDesignationLookups(file, ImportOptions.defaults());
    }

    public ImportSummary importDesignationLookups(MultipartFile file, ImportOptions options) {
        ImportOptions resolvedOptions = resolveOptions(options);
        CsvData csv = parseCsv(file);
        validateRequiredColumns(
                csv,
                List.of("stream"),
                List.of("band"),
                List.of("designation", "title", "name")
        );
        int created = 0;
        int updated = 0;
        Map<Integer, String> errorsByRow = new LinkedHashMap<>();
        Set<String> designationKeysInSheet = new HashSet<>();

        for (CsvRow row : csv.rows()) {
            try {
                String streamRaw = require(row, "stream");
                CurrentBand band = parseBand(require(row, "band"));
                String designation = require(row, "designation", "title", "name");

                String canonicalStream = BandStreamNormalizer.canonicalStreamLabel(streamRaw);
                String stream = firstNonBlank(canonicalStream, streamRaw);
                if (stream == null || stream.isBlank()) {
                    throw new IllegalArgumentException("Invalid stream value");
                }

                DesignationLookup.DesignationId id = new DesignationLookup.DesignationId(stream, band);
                String designationKey = normalizeKey(stream + "|" + band.name());
                if (!designationKeysInSheet.add(designationKey)) {
                    throw new IllegalArgumentException("Duplicate designation mapping in file for stream=" + stream + ", band=" + band);
                }
                boolean exists = designationLookupRepository.existsById(id);
                if (exists && resolvedOptions.rejectExisting()) {
                    throw new IllegalArgumentException("Designation mapping already exists for stream=" + stream + ", band=" + band);
                }

                if (resolvedOptions.validateOnly()) {
                    if (exists) {
                        updated++;
                    } else {
                        created++;
                    }
                } else {
                    DesignationLookup entity = new DesignationLookup();
                    entity.setId(id);
                    entity.setDesignation(designation.trim());
                    designationLookupRepository.save(entity);

                    if (exists) {
                        updated++;
                    } else {
                        created++;
                    }
                }
            } catch (Exception e) {
                registerError(errorsByRow, row.rowNumber(), e.getMessage());
            }
        }

        return buildSummary("designation-lookups", csv.rows().size(), created, updated, errorsByRow);
    }

    private ImportSummary buildSummary(String entity, int totalRows, int created, int updated, Map<Integer, String> errorsByRow) {
        List<RowError> errors = new ArrayList<>();
        int count = 0;
        for (Map.Entry<Integer, String> entry : errorsByRow.entrySet()) {
            errors.add(new RowError(entry.getKey(), entry.getValue()));
            count++;
            if (count >= MAX_ERROR_ROWS) {
                break;
            }
        }
        return new ImportSummary(entity, totalRows, created, updated, errorsByRow.size(), errors);
    }

    private static ImportOptions resolveOptions(ImportOptions options) {
        return options == null ? ImportOptions.defaults() : options;
    }

    @SafeVarargs
    private final void validateRequiredColumns(CsvData csv, List<String>... requiredAliasGroups) {
        List<String> missingColumns = new ArrayList<>();
        for (List<String> aliases : requiredAliasGroups) {
            boolean present = false;
            for (String alias : aliases) {
                if (csv.normalizedHeaders().contains(normalizeHeader(alias))) {
                    present = true;
                    break;
                }
            }
            if (!present && !aliases.isEmpty()) {
                missingColumns.add(aliases.get(0));
            }
        }
        if (!missingColumns.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required columns: " + String.join(", ", missingColumns)
            );
        }
    }

    private static String normalizeKey(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
    }

    private CsvData parseCsv(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required");
        }
        final String content;
        try {
            content = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded CSV");
        }

        String normalizedContent = content.startsWith("\uFEFF") ? content.substring(1) : content;
        char delimiter = detectDelimiter(normalizedContent);
        List<List<String>> records = parseRecords(normalizedContent, delimiter);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("CSV is empty");
        }
        int dataRows = Math.max(records.size() - 1, 0);
        if (dataRows > maxCsvRows) {
            throw new IllegalArgumentException("CSV row limit exceeded: " + dataRows + " rows (max " + maxCsvRows + ")");
        }

        List<String> rawHeaders = records.get(0);
        List<String> normalizedHeaders = new ArrayList<>(rawHeaders.size());
        Set<String> seenHeaders = new HashSet<>();
        Set<String> duplicateHeaders = new LinkedHashSet<>();
        for (String rawHeader : rawHeaders) {
            String normalized = normalizeHeader(rawHeader);
            normalizedHeaders.add(normalized);
            if (normalized.isBlank()) {
                continue;
            }
            if (!seenHeaders.add(normalized)) {
                duplicateHeaders.add(normalized);
            }
        }
        if (!duplicateHeaders.isEmpty()) {
            throw new IllegalArgumentException("Duplicate column names found in CSV header: " + String.join(", ", duplicateHeaders));
        }
        Set<String> normalizedHeaderSet = new LinkedHashSet<>(normalizedHeaders);

        List<CsvRow> rows = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> values = records.get(i);
            if (isBlankRow(values)) {
                continue;
            }
            Map<String, String> rowMap = new LinkedHashMap<>();
            Set<String> presentKeys = new LinkedHashSet<>();

            int max = Math.max(normalizedHeaders.size(), values.size());
            for (int idx = 0; idx < max; idx++) {
                String key = idx < normalizedHeaders.size() ? normalizedHeaders.get(idx) : "";
                if (key == null || key.isBlank()) {
                    continue;
                }
                String value = idx < values.size() ? values.get(idx) : "";
                rowMap.put(key, value == null ? "" : value.trim());
                presentKeys.add(key);
            }
            rows.add(new CsvRow(i + 1, rowMap, presentKeys));
        }

        return new CsvData(rawHeaders, normalizedHeaderSet, rows);
    }

    private List<List<String>> parseRecords(String csvContent, char delimiter) {
        List<List<String>> records = new ArrayList<>();
        List<String> currentRecord = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < csvContent.length(); i++) {
            char c = csvContent.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < csvContent.length() && csvContent.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (!inQuotes && c == delimiter) {
                currentRecord.add(currentField.toString());
                currentField.setLength(0);
                continue;
            }

            if (!inQuotes && (c == '\n' || c == '\r')) {
                currentRecord.add(currentField.toString());
                currentField.setLength(0);
                records.add(currentRecord);
                currentRecord = new ArrayList<>();

                if (c == '\r' && i + 1 < csvContent.length() && csvContent.charAt(i + 1) == '\n') {
                    i++;
                }
                continue;
            }

            currentField.append(c);
        }

        currentRecord.add(currentField.toString());
        boolean hasAnyValue = currentRecord.stream().anyMatch(v -> v != null && !v.isBlank());
        if (hasAnyValue || !records.isEmpty()) {
            records.add(currentRecord);
        }
        return records;
    }

    private static char detectDelimiter(String content) {
        if (content == null || content.isBlank()) {
            return ',';
        }
        String[] lines = content.split("\\R", -1);
        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }
            int commaCount = countDelimiterOutsideQuotes(line, ',');
            int semicolonCount = countDelimiterOutsideQuotes(line, ';');
            int tabCount = countDelimiterOutsideQuotes(line, '\t');

            if (tabCount > semicolonCount && tabCount > commaCount && tabCount > 0) {
                return '\t';
            }
            if (semicolonCount > commaCount && semicolonCount > 0) {
                return ';';
            }
            if (commaCount > 0) {
                return ',';
            }
            break;
        }
        return ',';
    }

    private static int countDelimiterOutsideQuotes(String line, char delimiter) {
        int count = 0;
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (!inQuotes && c == delimiter) {
                count++;
            }
        }
        return count;
    }

    private void ensureDesignationLookupExists(String streamRaw, CurrentBand band, String designationRaw) {
        if (band == null) {
            return;
        }
        String canonical = BandStreamNormalizer.canonicalStreamLabel(streamRaw);
        String stream = firstNonBlank(canonical, streamRaw);
        if (stream == null || stream.isBlank()) {
            return;
        }

        DesignationLookup.DesignationId id = new DesignationLookup.DesignationId(stream, band);
        String designation = firstNonBlank(designationRaw, band.name() + " - " + stream);
        if (designation == null || designation.isBlank()) {
            return;
        }

        Optional<DesignationLookup> existing = designationLookupRepository.findById(id);
        if (existing.isPresent()) {
            String currentDesignation = existing.get().getDesignation();
            if (currentDesignation == null || !currentDesignation.equals(designation)) {
                DesignationLookup updated = existing.get();
                updated.setDesignation(designation);
                designationLookupRepository.save(updated);
            }
            return;
        }

        DesignationLookup entity = new DesignationLookup();
        entity.setId(id);
        entity.setDesignation(designation);
        designationLookupRepository.save(entity);
    }

    private static boolean isBlankRow(List<String> values) {
        if (values == null || values.isEmpty()) {
            return true;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static String normalizeHeader(String raw) {
        if (raw == null) {
            return "";
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return NON_ALNUM.matcher(normalized).replaceAll("");
    }

    private static String require(CsvRow row, String... aliases) {
        String value = value(row, aliases);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + (aliases.length > 0 ? aliases[0] : "field"));
        }
        return value;
    }

    private static String value(CsvRow row, String... aliases) {
        String raw = rawValue(row, aliases);
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String rawValue(CsvRow row, String... aliases) {
        if (row == null || aliases == null) {
            return null;
        }
        for (String alias : aliases) {
            String key = normalizeHeader(alias);
            if (row.values().containsKey(key)) {
                return row.values().get(key);
            }
        }
        return null;
    }

    private static boolean hasAnyHeader(CsvRow row, String... aliases) {
        if (row == null || aliases == null) {
            return false;
        }
        for (String alias : aliases) {
            if (row.presentKeys().contains(normalizeHeader(alias))) {
                return true;
            }
        }
        return false;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isBlank()) {
                return trimmed;
            }
        }
        return null;
    }

    private static CurrentBand parseBand(String raw) {
        return BandStreamNormalizer.parseBand(raw)
                .orElseThrow(() -> new IllegalArgumentException("Invalid band: " + raw));
    }

    private static CurrentStream parseStream(String raw) {
        return BandStreamNormalizer.parseStream(raw)
                .orElseThrow(() -> new IllegalArgumentException("Invalid stream: " + raw));
    }

    private static EmployeeRole parseRoleNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        for (EmployeeRole role : EmployeeRole.values()) {
            if (role.name().equalsIgnoreCase(raw.trim())) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid employee role: " + raw);
    }

    private static Integer parseIntegerNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer value: " + raw);
        }
    }

    private static BigDecimal parseDecimal(String raw) {
        try {
            return new BigDecimal(raw.trim());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid decimal value: " + raw);
        }
    }

    private static Boolean parseBooleanNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "1", "true", "yes", "y" -> true;
            case "0", "false", "no", "n" -> false;
            default -> throw new IllegalArgumentException("Invalid boolean value: " + raw);
        };
    }

    private static void registerError(Map<Integer, String> errorsByRow, int rowNumber, String message) {
        if (errorsByRow.containsKey(rowNumber)) {
            return;
        }
        String safeMessage = message == null || message.isBlank() ? "Import failed" : message;
        errorsByRow.put(rowNumber, safeMessage);
    }

    private record CsvData(List<String> headers, Set<String> normalizedHeaders, List<CsvRow> rows) {
    }

    private record CsvRow(int rowNumber, Map<String, String> values, Set<String> presentKeys) {
    }

    private record EmployeeRelationUpdate(int rowNumber,
                                          String employeeId,
                                          boolean managerColumnPresent,
                                          String managerRaw,
                                          boolean updatedByColumnPresent,
                                          String updatedByRaw) {
    }

    public record ImportSummary(String entity,
                                int totalRows,
                                int created,
                                int updated,
                                int failed,
                                List<RowError> errors) {
    }

    public record RowError(int rowNumber, String message) {
    }

    public record ImportOptions(boolean validateOnly, boolean rejectExisting) {
        public static ImportOptions defaults() {
            return new ImportOptions(false, false);
        }
    }
}
