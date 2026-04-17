package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LearningFacadeJpaRepository extends JpaRepository<LearningFacade, Long> {


    @EntityGraph(attributePaths = {"axes", "axes.actions"})
    @Query("SELECT f FROM LearningFacade f WHERE f.user.id = :userId")
    Optional<LearningFacade> findByUserId(@Param("userId") Long userId);

    boolean existsByUserId(Long userId);
}
