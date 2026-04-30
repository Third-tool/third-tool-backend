package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LearningFacadeJpaRepository extends JpaRepository<LearningFacade, Long> {

    // axes.topics까지 fetch하면 두 컬렉션 동시 fetch로 MultipleBagFetchException 발생.
    // 하위 topics는 default_batch_fetch_size로 분리 로딩한다.
    @EntityGraph(attributePaths = {"axes"})
    @Query("SELECT f FROM LearningFacade f WHERE f.user.id = :userId")
    Optional<LearningFacade> findByUserId(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);
}
