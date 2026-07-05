package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.LearningLayer;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;


public interface LearningFacadeRepository {

    Optional<LearningFacade> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    LearningFacade save(LearningFacade facade);

    /**
     * Cross-BC read — axisId 집합에 대응하는 axisName Map을 반환한다.
     * Deck BC가 응답 DTO에 axisName을 동봉할 때 사용. 미존재 axisId는 결과에 포함되지 않는다.
     */
    Map<Long, String> findAxisNamesByIds(Collection<Long> axisIds);

    /**
     * LT-E5-S5-2 — Card·Review BC가 Deck 참조 완전 폐기 후 axis 조회에 사용.
     * axisId로 LearningAxis 단건 조회. @SQLRestriction("deleted_at IS NULL") 자동 필터 적용.
     */
    Optional<LearningAxis> findAxisById(Long axisId);

    /**
     * LT-E5-S5-2 — axisId로 소유 사용자 id 조회.
     * Card 생성·Review 세션 시작 시 소유권 · createdMode 조회에 사용.
     * axis→facade→user 경로로 이관.
     */
    Optional<Long> findUserIdByAxisId(Long axisId);

    /**
     * LT-E6-S6-3 (M5) — layerId로 LearningLayer 단건 조회.
     * @SQLRestriction("deleted_at IS NULL") 자동 필터 적용.
     */
    Optional<LearningLayer> findLayerById(Long layerId);

    /**
     * LT-E6-S6-3 (M5) — layerId로 소유 사용자 id 조회 (layer→facade→user 경로).
     */
    Optional<Long> findUserIdByLayerId(Long layerId);
}