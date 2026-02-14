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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class KpiDefinitionService {

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

            if (def.getCreatedAt() == null) {
                def.setCreatedAt(LocalDateTime.now());
            }

            return kpiDefinitionRepository.save(def);
        } catch (CrudValidationException e) {
            throw e;
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
