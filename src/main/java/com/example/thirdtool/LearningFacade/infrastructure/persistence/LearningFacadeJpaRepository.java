package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.domain.model.LearningLayer;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LearningFacadeJpaRepository extends JpaRepository<LearningFacade, Long> {

    // axes.topics까지 fetch하면 두 컬렉션 동시 fetch로 MultipleBagFetchException 발생.
    // 하위 topics는 default_batch_fetch_size로 분리 로딩한다.
    @EntityGraph(attributePaths = {"axes"})
    @Query("SELECT f FROM LearningFacade f WHERE f.user.id = :userId")
    Optional<LearningFacade> findByUserId(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);

    /**
     * Cross-BC read — axisId 목록에 대응하는 (id, name) 쌍 일괄 조회.
     * Deck BC가 응답 DTO에 axisName을 동봉할 때 사용. facade 컨텍스트 없이 axisId만으로 조회.
     */
    @Query("SELECT a.id AS id, a.name AS name FROM LearningAxis a WHERE a.id IN :axisIds")
    List<AxisIdNameProjection> findAxisIdNamePairsByIdIn(@Param("axisIds") Collection<Long> axisIds);

    /**
     * LT-E5-S5-2 — axisId로 LearningAxis 단건 조회.
     * @SQLRestriction("deleted_at IS NULL") 자동 필터 적용.
     */
    @Query("SELECT a FROM LearningAxis a WHERE a.id = :axisId")
    Optional<LearningAxis> findAxisById(@Param("axisId") Long axisId);

    /**
     * LT-E5-S5-2 — axisId로 소유 사용자 id 조회 (axis→facade→user).
     * Card 생성·Review 세션 시작 시 소유권·createdMode 조회에 사용.
     */
    @Query("SELECT a.facade.user.id FROM LearningAxis a WHERE a.id = :axisId")
    Optional<Long> findUserIdByAxisId(@Param("axisId") Long axisId);

    /**
     * LT-E6-S6-3 (M5) — layerId로 LearningLayer 단건 조회.
     * @SQLRestriction 자동 필터 적용.
     */
    @Query("SELECT l FROM LearningLayer l WHERE l.id = :layerId")
    Optional<LearningLayer> findLayerById(@Param("layerId") Long layerId);

    /**
     * LT-E6-S6-3 (M5) — layerId로 소유 사용자 id 조회 (layer→facade→user).
     */
    @Query("SELECT l.facade.user.id FROM LearningLayer l WHERE l.id = :layerId")
    Optional<Long> findUserIdByLayerId(@Param("layerId") Long layerId);

    interface AxisIdNameProjection {
        Long getId();
        String getName();
    }
}
