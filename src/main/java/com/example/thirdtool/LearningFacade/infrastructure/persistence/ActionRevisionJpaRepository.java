package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.ActionRevision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActionRevisionJpaRepository extends JpaRepository<ActionRevision, Long> {

    List<ActionRevision> findByActionIdOrderByRevisedAtAsc(Long actionId);
}

