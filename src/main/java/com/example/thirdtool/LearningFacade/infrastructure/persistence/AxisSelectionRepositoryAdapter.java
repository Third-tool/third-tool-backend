package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisSelection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AxisSelectionRepositoryAdapter implements AxisSelectionRepository {

    private final AxisSelectionJpaRepository jpa;

    @Override
    public Optional<AxisSelection> findById(Long selectionId) {
        return jpa.findById(selectionId);
    }

    @Override
    public List<AxisSelection> findByAxisIdOrderByCreatedAtDesc(Long axisId) {
        return jpa.findByAxisIdOrderByCreatedAtDesc(axisId);
    }

    @Override
    public AxisSelection save(AxisSelection selection) {
        return jpa.save(selection);
    }
}
