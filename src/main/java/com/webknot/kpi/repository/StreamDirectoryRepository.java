package com.webknot.kpi.repository;

import com.webknot.kpi.models.CurrentStream;
import com.webknot.kpi.models.StreamDirectory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StreamDirectoryRepository extends JpaRepository<StreamDirectory, CurrentStream> {
}

