package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.ActionMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActionMaterialJpaRepository extends JpaRepository<ActionMaterial, Long> {

    @Query("""
            SELECT am FROM ActionMaterial am
            JOIN FETCH am.material
            JOIN FETCH am.action a
            JOIN FETCH a.axis
            WHERE am.action.id = :actionId
            """)
    List<ActionMaterial> findByActionId(@Param("actionId") Long actionId);

    @Query("""
            SELECT am FROM ActionMaterial am
            JOIN FETCH am.action
            WHERE am.action.id = :actionId
              AND am.material.id = :materialId
            """)
    Optional<ActionMaterial> findByActionIdAndMaterialId(
            @Param("actionId") Long actionId,
            @Param("materialId") Long materialId
                                                        );

    boolean existsByActionIdAndMaterialId(Long actionId, Long materialId);

    @Query("SELECT COUNT(am) FROM ActionMaterial am WHERE am.action.id = :actionId")
    long countByActionId(@Param("actionId") Long actionId);
}
