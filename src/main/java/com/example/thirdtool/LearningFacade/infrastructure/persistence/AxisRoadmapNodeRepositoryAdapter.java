package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisRoadmapNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AxisRoadmapNodeRepositoryAdapter implements AxisRoadmapNodeRepository {

    private final AxisRoadmapNodeJpaRepository jpa;

    @Override
    public Optional<AxisRoadmapNode> findById(Long nodeId) {
        return jpa.findById(nodeId);
    }

    @Override
    public List<AxisRoadmapNode> findByAxisIdOrderByDisplayOrderAsc(Long axisId) {
        return jpa.findByAxisIdOrderByDisplayOrderAsc(axisId);
    }

    @Override
    public AxisRoadmapNode save(AxisRoadmapNode node) {
        return jpa.save(node);
    }
}
