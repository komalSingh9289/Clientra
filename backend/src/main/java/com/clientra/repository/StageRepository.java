package com.clientra.repository;

import com.clientra.entity.PipelineStage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StageRepository extends JpaRepository<PipelineStage, Integer> {
    List<PipelineStage> findAllByOrderBySequenceOrderAsc();
}
