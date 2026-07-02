package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AxisSelectionJpaRepository extends JpaRepository<AxisSelection, Long> {

    List<AxisSelection> findByAxisIdOrderByCreatedAtDesc(Long axisId);
}
