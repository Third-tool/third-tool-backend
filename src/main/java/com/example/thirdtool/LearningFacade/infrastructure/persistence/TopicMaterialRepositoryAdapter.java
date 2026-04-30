package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
import com.example.thirdtool.LearningFacade.domain.model.TopicMaterial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TopicMaterialRepositoryAdapter implements TopicMaterialRepository {

    private final TopicMaterialJpaRepository jpa;

    @Override
    public List<TopicMaterial> findByTopicId(Long topicId) {
        return jpa.findByTopicId(topicId);
    }

    @Override
    public List<TopicMaterial> findByMaterialId(Long materialId) {
        return jpa.findByMaterialId(materialId);
    }

    @Override
    public Optional<TopicMaterial> findByTopicIdAndMaterialId(Long topicId, Long materialId) {
        return jpa.findByTopicIdAndMaterialId(topicId, materialId);
    }

    @Override
    public boolean existsByTopicIdAndMaterialId(Long topicId, Long materialId) {
        return jpa.existsByTopicIdAndMaterialId(topicId, materialId);
    }

    @Override
    public long countByTopicId(Long topicId) {
        return jpa.countByTopicId(topicId);
    }

    @Override
    public boolean existsByTopicIdAndMaterialProficiencyLevel(Long topicId, ProficiencyLevel level) {
        return jpa.existsByTopicIdAndMaterialProficiencyLevel(topicId, level);
    }

    @Override
    public TopicMaterial save(TopicMaterial topicMaterial) {
        return jpa.save(topicMaterial);
    }

    @Override
    public void delete(TopicMaterial topicMaterial) {
        jpa.delete(topicMaterial);
    }
}
