package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
import com.example.thirdtool.LearningFacade.domain.model.TopicMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TopicMaterialJpaRepository extends JpaRepository<TopicMaterial, Long> {

    @Query("""
            SELECT tm FROM TopicMaterial tm
            JOIN FETCH tm.material
            JOIN FETCH tm.topic t
            JOIN FETCH t.axis
            WHERE tm.topic.id = :topicId
            """)
    List<TopicMaterial> findByTopicId(@Param("topicId") Long topicId);

    @Query("""
            SELECT tm FROM TopicMaterial tm
            JOIN FETCH tm.topic t
            JOIN FETCH t.axis
            WHERE tm.material.id = :materialId
            """)
    List<TopicMaterial> findByMaterialId(@Param("materialId") Long materialId);

    @Query("""
            SELECT tm FROM TopicMaterial tm
            JOIN FETCH tm.topic
            WHERE tm.topic.id = :topicId
              AND tm.material.id = :materialId
            """)
    Optional<TopicMaterial> findByTopicIdAndMaterialId(
            @Param("topicId") Long topicId,
            @Param("materialId") Long materialId
    );

    boolean existsByTopicIdAndMaterialId(Long topicId, Long materialId);

    @Query("SELECT COUNT(tm) FROM TopicMaterial tm WHERE tm.topic.id = :topicId")
    long countByTopicId(@Param("topicId") Long topicId);

    @Query("""
            SELECT CASE WHEN COUNT(tm) > 0 THEN TRUE ELSE FALSE END
            FROM TopicMaterial tm
            WHERE tm.topic.id = :topicId
              AND tm.material.proficiencyLevel = :level
            """)
    boolean existsByTopicIdAndMaterialProficiencyLevel(
            @Param("topicId") Long topicId,
            @Param("level") ProficiencyLevel level
    );
}
