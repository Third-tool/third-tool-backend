package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.ActionMaterial;

import java.util.List;
import java.util.Optional;

public interface ActionMaterialRepository {

    List<ActionMaterial> findByActionId(Long actionId);

    Optional<ActionMaterial> findByActionIdAndMaterialId(Long actionId, Long materialId);

    long countByActionId(Long actionId);


    boolean existsByActionIdAndMaterialId(Long actionId, Long materialId);

    ActionMaterial save(ActionMaterial actionMaterial);

    void delete(ActionMaterial actionMaterial);
}