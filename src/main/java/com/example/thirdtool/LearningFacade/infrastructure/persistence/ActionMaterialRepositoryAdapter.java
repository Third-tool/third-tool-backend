package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.ActionMaterial;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ActionMaterialRepositoryAdapter implements ActionMaterialRepository {

    private final ActionMaterialJpaRepository jpa;

    @Override
    public List<ActionMaterial> findByActionId(Long actionId) {
        return jpa.findByActionId(actionId);
    }

    @Override
    public Optional<ActionMaterial> findByActionIdAndMaterialId(Long actionId, Long materialId) {
        return jpa.findByActionIdAndMaterialId(actionId, materialId);
    }

    @Override
    public long countByActionId(Long actionId) {
        return jpa.countByActionId(actionId);
    }

    @Override
    public boolean existsByActionIdAndMaterialId(Long actionId, Long materialId) {
        return jpa.existsByActionIdAndMaterialId(actionId, materialId);
    }

    @Override
    public ActionMaterial save(ActionMaterial actionMaterial) {
        return jpa.save(actionMaterial);
    }

    @Override
    public void delete(ActionMaterial actionMaterial) {
        jpa.delete(actionMaterial);
    }
}
