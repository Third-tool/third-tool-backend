package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.LearningMaterial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class LearningMaterialRepositoryAdapter implements LearningMaterialRepository {

    private final LearningMaterialJpaRepository jpa;

    @Override
    public Optional<LearningMaterial> findById(Long materialId) {
        return jpa.findByIdWithMappings(materialId);
    }

    @Override
    public List<LearningMaterial> findByFacadeId(Long facadeId) {
        return jpa.findByFacadeId(facadeId);
    }

    @Override
    public LearningMaterial save(LearningMaterial material) {
        return jpa.save(material);
    }

    @Override
    public void delete(LearningMaterial material) {
        jpa.delete(material);
    }
}
