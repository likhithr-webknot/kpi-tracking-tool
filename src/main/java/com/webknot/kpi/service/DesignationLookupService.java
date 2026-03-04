package com.webknot.kpi.service;

import com.webknot.kpi.models.CurrentBand;
import com.webknot.kpi.models.CurrentStream;
import com.webknot.kpi.models.DesignationLookup;
import com.webknot.kpi.repository.DesignationLookupRepository;
import com.webknot.kpi.util.BandStreamNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class DesignationLookupService {

    private static final Logger log = LoggerFactory.getLogger(DesignationLookupService.class);

    private final DesignationLookupRepository designationLookupRepository;

    public DesignationLookupService(DesignationLookupRepository designationLookupRepository) {
        this.designationLookupRepository = designationLookupRepository;
    }

    @Transactional(readOnly = true, timeout = 5)
    @Cacheable(value = "designation-lookup", key = "#stream + '-' + #band.name()")
    public Optional<DesignationLookup> getByStreamAndBand(String stream, CurrentBand band) {
        String normalizedStream = normalizeStreamToCanonical(stream);
        log.debug("Looking up designation for stream={} (normalized={}), band={}", stream, normalizedStream, band);
        return designationLookupRepository.findByStreamAndBand(normalizedStream, band);
    }

    @Transactional(readOnly = true, timeout = 5)
    @Cacheable(value = "designation-lookups-by-stream", key = "#stream")
    public List<DesignationLookup> getByStream(String stream) {
        String normalizedStream = normalizeStreamToCanonical(stream);
        log.debug("Looking up designations for stream={} (normalized={})", stream, normalizedStream);
        return designationLookupRepository.findByStream(normalizedStream);
    }

    @Transactional(readOnly = true, timeout = 5)
    @Cacheable(value = "designation-lookups-by-band", key = "#band.name()")
    public List<DesignationLookup> getByBand(CurrentBand band) {
        log.debug("Looking up designations for band={}", band);
        return designationLookupRepository.findByBand(band);
    }

    @Transactional(readOnly = true, timeout = 5)
    public List<DesignationLookup> getAll() {
        return designationLookupRepository.findAll();
    }

    private String normalizeStreamToCanonical(String stream) {
        Optional<CurrentStream> parsed = BandStreamNormalizer.parseStream(stream);
        return parsed.map(CurrentStream::name).orElse(stream != null ? stream.trim() : "");
    }
}
