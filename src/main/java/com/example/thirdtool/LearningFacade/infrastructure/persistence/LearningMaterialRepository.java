package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.LearningMaterial;

import java.util.List;
import java.util.Optional;

public interface LearningMaterialRepository {

    Optional<LearningMaterial> findById(Long materialId);

    List<LearningMaterial> findByFacadeId(Long facadeId);

    LearningMaterial save(LearningMaterial material);

    void delete(LearningMaterial material);
}

