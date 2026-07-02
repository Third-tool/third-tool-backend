package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.AxisRoadmapNode;

import java.util.List;
import java.util.Optional;

/**
 * Roadmap 노드 Repository Port (Story-LT-E3-S3-8).
 */
public interface AxisRoadmapNodeRepository {

    Optional<AxisRoadmapNode> findById(Long nodeId);

    List<AxisRoadmapNode> findByAxisIdOrderByDisplayOrderAsc(Long axisId);

    AxisRoadmapNode save(AxisRoadmapNode node);
}
