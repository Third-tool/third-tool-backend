package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;

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
}