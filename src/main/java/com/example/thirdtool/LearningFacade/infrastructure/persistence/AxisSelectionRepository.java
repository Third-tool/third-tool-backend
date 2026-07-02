package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisSelection;

import java.util.List;
import java.util.Optional;

/**
 * Selection 컨테이너 Repository Port (Story-LT-E3-S3-11).
 */
public interface AxisSelectionRepository {

    Optional<AxisSelection> findById(Long selectionId);

    List<AxisSelection> findByAxisIdOrderByCreatedAtDesc(Long axisId);

    AxisSelection save(AxisSelection selection);
}
