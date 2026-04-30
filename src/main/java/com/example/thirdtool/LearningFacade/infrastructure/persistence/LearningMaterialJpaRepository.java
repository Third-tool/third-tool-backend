package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.LearningMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LearningMaterialJpaRepository extends JpaRepository<LearningMaterial, Long> {

    @Query("""
            SELECT DISTINCT m FROM LearningMaterial m
            LEFT JOIN FETCH m.topicMappings tm
            LEFT JOIN FETCH tm.topic t
            LEFT JOIN FETCH t.axis
            WHERE m.id = :materialId
            """)
    Optional<LearningMaterial> findByIdWithMappings(@Param("materialId") Long materialId);

    @Query("""
            SELECT DISTINCT m FROM LearningMaterial m
            LEFT JOIN FETCH m.topicMappings tm
            LEFT JOIN FETCH tm.topic t
            LEFT JOIN FETCH t.axis
            WHERE m.facade.id = :facadeId
            ORDER BY m.createdAt ASC
            """)
    List<LearningMaterial> findByFacadeId(@Param("facadeId") Long facadeId);
}
