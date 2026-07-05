package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.LearningLayer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LearningFacadeRepositoryAdapter implements LearningFacadeRepository {

    private final LearningFacadeJpaRepository jpa;

    @Override
    public Optional<LearningFacade> findByUserId(Long userId) {
        // @EntityGraph({"axes", "axes.actions"}) 적용 → 이중 컬렉션 SELECT 분리 로딩
        return jpa.findByUserId(userId);
    }

    @Override
    public boolean existsByUserId(Long userId) {
        return jpa.existsByUserId(userId);
    }

    @Override
    public LearningFacade save(LearningFacade facade) {
        return jpa.save(facade);
    }

    @Override
    public Map<Long, String> findAxisNamesByIds(Collection<Long> axisIds) {
        if (axisIds == null || axisIds.isEmpty()) {
            return Map.of();
        }
        return jpa.findAxisIdNamePairsByIdIn(axisIds).stream()
                .collect(Collectors.toMap(
                        LearningFacadeJpaRepository.AxisIdNameProjection::getId,
                        LearningFacadeJpaRepository.AxisIdNameProjection::getName
                ));
    }

    @Override
    public Optional<LearningAxis> findAxisById(Long axisId) {
        return jpa.findAxisById(axisId);
    }

    @Override
    public Optional<Long> findUserIdByAxisId(Long axisId) {
        return jpa.findUserIdByAxisId(axisId);
    }

    @Override
    public Optional<LearningLayer> findLayerById(Long layerId) {
        return jpa.findLayerById(layerId);
    }

    @Override
    public Optional<Long> findUserIdByLayerId(Long layerId) {
        return jpa.findUserIdByLayerId(layerId);
    }
}
