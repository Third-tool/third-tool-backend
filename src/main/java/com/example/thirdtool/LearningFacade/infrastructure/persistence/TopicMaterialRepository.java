package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
import com.example.thirdtool.LearningFacade.domain.model.TopicMaterial;

import java.util.List;
import java.util.Optional;

public interface TopicMaterialRepository {

    List<TopicMaterial> findByTopicId(Long topicId);

    List<TopicMaterial> findByMaterialId(Long materialId);

    Optional<TopicMaterial> findByTopicIdAndMaterialId(Long topicId, Long materialId);

    boolean existsByTopicIdAndMaterialId(Long topicId, Long materialId);

    long countByTopicId(Long topicId);

    boolean existsByTopicIdAndMaterialProficiencyLevel(Long topicId, ProficiencyLevel level);

    TopicMaterial save(TopicMaterial topicMaterial);

    void delete(TopicMaterial topicMaterial);
}
