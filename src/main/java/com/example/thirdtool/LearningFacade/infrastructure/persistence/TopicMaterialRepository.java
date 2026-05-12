package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
import com.example.thirdtool.LearningFacade.domain.model.TopicMaterial;

import java.util.List;
import java.util.Optional;

public interface TopicMaterialRepository {

    List<TopicMaterial> findByTopicId(Long topicId);

    /**
     * 주제 id 리스트로 매핑을 일괄 조회한다 — Story 4-3 비중 집계 진입점.
     * 자료(material)를 함께 fetch하여 N+1 회피.
     */
    List<TopicMaterial> findByTopicIdIn(List<Long> topicIds);

    List<TopicMaterial> findByMaterialId(Long materialId);

    Optional<TopicMaterial> findByTopicIdAndMaterialId(Long topicId, Long materialId);

    boolean existsByTopicIdAndMaterialId(Long topicId, Long materialId);

    long countByTopicId(Long topicId);

    boolean existsByTopicIdAndMaterialProficiencyLevel(Long topicId, ProficiencyLevel level);

    TopicMaterial save(TopicMaterial topicMaterial);

    void delete(TopicMaterial topicMaterial);
}
