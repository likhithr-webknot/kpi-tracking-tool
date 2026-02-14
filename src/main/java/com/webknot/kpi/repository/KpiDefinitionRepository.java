package com.webknot.kpi.repository;

import com.webknot.kpi.models.CurrentBand;
import com.webknot.kpi.models.CurrentStream;
import com.webknot.kpi.models.KpiDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KpiDefinitionRepository extends JpaRepository<KpiDefinition, Long> {

    Optional<KpiDefinition> findByBandAndStreamAndKpiName(CurrentBand band, CurrentStream stream, String kpiName);

    boolean existsByBandAndStreamAndKpiName(CurrentBand band, CurrentStream stream, String kpiName);

    List<KpiDefinition> findByBandAndStream(CurrentBand band, CurrentStream stream);

    List<KpiDefinition> findByBand(CurrentBand band);

    List<KpiDefinition> findByStream(CurrentStream stream);
}
