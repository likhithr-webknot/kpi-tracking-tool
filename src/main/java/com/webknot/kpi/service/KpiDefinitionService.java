package com.webknot.kpi.service;

import com.webknot.kpi.exceptions.CrudOperationException;
import com.webknot.kpi.exceptions.CrudValidationErrorCode;
import com.webknot.kpi.exceptions.CrudValidationException;
import com.webknot.kpi.models.CurrentBand;
import com.webknot.kpi.models.CurrentStream;
import com.webknot.kpi.models.KpiDefinition;
import com.webknot.kpi.repository.KpiDefinitionRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class KpiDefinitionService {
    private static final BigDecimal MAX_TOTAL_WEIGHTAGE = new BigDecimal("100.00");

    private final KpiDefinitionRepository kpiDefinitionRepository;
    private final Validator validator;

    public KpiDefinitionService(KpiDefinitionRepository kpiDefinitionRepository, Validator validator) {
        this.kpiDefinitionRepository = kpiDefinitionRepository;
        this.validator = validator;
    }

    private void checkForNull(KpiDefinition def) {
        if (def == null) {
            throw CrudOperationException.asNullEntity(KpiDefinition.class);
        }
    }

    private void validate(KpiDefinition def) throws CrudValidationException {
        Set<ConstraintViolation<KpiDefinition>> violations = validator.validate(def);
        if (!violations.isEmpty()) {
            throw CrudValidationException.asFailedValidationOperation(KpiDefinition.class, violations);
        }
    }

    private BigDecimal totalWeightage(CurrentBand band, CurrentStream stream, Long excludeId) {
        return kpiDefinitionRepository.findByBandAndStream(band, stream).stream()
                .filter(k -> excludeId == null || !k.getId().equals(excludeId))
                .map(KpiDefinition::getWeightage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void validateWeightageCap(CurrentBand band, CurrentStream stream, BigDecimal weightage, Long excludeId) {
        BigDecimal currentTotal = totalWeightage(band, stream, excludeId);
        BigDecimal proposedTotal = currentTotal.add(weightage);
        if (proposedTotal.compareTo(MAX_TOTAL_WEIGHTAGE) > 0) {
            throw new CrudValidationException(KpiDefinition.class,
                    "Total weightage for " + band + " in " + stream +
                            " cannot exceed 100.00. Current: " + currentTotal + ", requested: " + weightage,
                    CrudValidationErrorCode.DATA_VALIDATION);
        }
    }

    private CrudValidationException mapConstraintViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        if (message != null && message.contains("kpi_definitions_weightage_check")) {
            return new CrudValidationException(KpiDefinition.class,
                    "Database check constraint 'kpi_definitions_weightage_check' is still enforcing old weightage range. " +
                            "Update DB constraint to allow values up to 100.00.",
                    CrudValidationErrorCode.DATA_VALIDATION);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<KpiDefinition> getAll() {
        try {
            return kpiDefinitionRepository.findAll();
        } catch (Exception e) {
            throw CrudOperationException.asFailedGetOperation(KpiDefinition.class, e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<KpiDefinition> getById(Long id) {
        if (id == null || id <= 0) {
            throw new CrudValidationException(KpiDefinition.class,
                    "Invalid KPI Definition id. Must be > 0",
                    CrudValidationErrorCode.INVALID_IDENTIFIER);
        }
        try {
            return kpiDefinitionRepository.findById(id);
        } catch (Exception e) {
            throw CrudOperationException.asFailedGetOperation(KpiDefinition.class, e);
        }
    }

    @Transactional
    public KpiDefinition add(KpiDefinition def) {
        checkForNull(def);
        validate(def);

        try {
            if (def.getId() != null) {
                throw new CrudValidationException(KpiDefinition.class,
                        "ID must be null while creating a KPI Definition",
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            if (kpiDefinitionRepository.existsByBandAndStreamAndKpiName(def.getBand(), def.getStream(), def.getKpiName())) {
                throw new CrudValidationException(KpiDefinition.class,
                        "KPI Definition already exists for band=" + def.getBand() +
                                ", stream=" + def.getStream() +
                                ", kpiName=" + def.getKpiName(),
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            validateWeightageCap(def.getBand(), def.getStream(), def.getWeightage(), null);

            if (def.getCreatedAt() == null) {
                def.setCreatedAt(LocalDateTime.now());
            }

            return kpiDefinitionRepository.save(def);
        } catch (CrudValidationException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            CrudValidationException mapped = mapConstraintViolation(e);
            if (mapped != null) {
                throw mapped;
            }
            throw CrudOperationException.asFailedAddOperation(KpiDefinition.class, e);
        } catch (Exception e) {
            throw CrudOperationException.asFailedAddOperation(KpiDefinition.class, e);
        }
    }

    @Transactional
    public KpiDefinition update(KpiDefinition def) {
        checkForNull(def);
        validate(def);

        try {
            if (def.getId() == null || def.getId() <= 0) {
                throw new CrudValidationException(KpiDefinition.class,
                        "ID is required for update",
                        CrudValidationErrorCode.INVALID_IDENTIFIER);
            }

            KpiDefinition existing = kpiDefinitionRepository.findById(def.getId())
                    .orElseThrow(() -> CrudOperationException.asEntityNotFound(KpiDefinition.class, def.getId()));

            boolean uniqueConflict =
                    kpiDefinitionRepository.findByBandAndStreamAndKpiName(def.getBand(), def.getStream(), def.getKpiName())
                            .filter(other -> !other.getId().equals(existing.getId()))
                            .isPresent();

            if (uniqueConflict) {
                throw new CrudValidationException(KpiDefinition.class,
                        "Another KPI Definition already exists for band=" + def.getBand() +
                                ", stream=" + def.getStream() +
                                ", kpiName=" + def.getKpiName(),
                        CrudValidationErrorCode.DATA_VALIDATION);
            }

            validateWeightageCap(def.getBand(), def.getStream(), def.getWeightage(), existing.getId());

            existing.setBand(def.getBand());
            existing.setStream(def.getStream());
            existing.setKpiName(def.getKpiName());
            existing.setWeightage(def.getWeightage());
            existing.setDescription(def.getDescription());

            return kpiDefinitionRepository.save(existing);
        } catch (CrudValidationException e) {
            throw e;
        } catch (CrudOperationException e) {
            throw e;
        } catch (DataIntegrityViolationException e) {
            CrudValidationException mapped = mapConstraintViolation(e);
            if (mapped != null) {
                throw mapped;
            }
            throw CrudOperationException.asFailedUpdateOperation(KpiDefinition.class, e);
        } catch (Exception e) {
            throw CrudOperationException.asFailedUpdateOperation(KpiDefinition.class, e);
        }
    }

    @Transactional
    public void delete(Long id) {
        if (id == null || id <= 0) {
            throw new CrudValidationException(KpiDefinition.class,
                    "Invalid KPI Definition id. Must be > 0",
                    CrudValidationErrorCode.INVALID_IDENTIFIER);
        }

        try {
            if (!kpiDefinitionRepository.existsById(id)) {
                throw CrudOperationException.asEntityNotFound(KpiDefinition.class, id);
            }
            kpiDefinitionRepository.deleteById(id);
        } catch (CrudValidationException e) {
            throw e;
        } catch (CrudOperationException e) {
            throw e;
        } catch (Exception e) {
            throw CrudOperationException.asFailedDeleteOperation(KpiDefinition.class, e);
        }
    }

    @Transactional(readOnly = true)
    public List<KpiDefinition> search(CurrentBand band, CurrentStream stream) {
        try {
            if (band != null && stream != null) {
                return kpiDefinitionRepository.findByBandAndStream(band, stream);
            } else if (band != null) {
                return kpiDefinitionRepository.findByBand(band);
            } else if (stream != null) {
                return kpiDefinitionRepository.findByStream(stream);
            } else {
                return kpiDefinitionRepository.findAll();
            }
        } catch (Exception e) {
            throw CrudOperationException.asFailedGetOperation(KpiDefinition.class, e);
        }
    }
}
