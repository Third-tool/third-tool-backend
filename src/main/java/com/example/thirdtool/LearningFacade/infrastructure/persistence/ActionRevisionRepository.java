package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.ActionRevision;

import java.util.List;

public interface ActionRevisionRepository {

    ActionRevision save(ActionRevision revision);

    List<ActionRevision> findByActionIdOrderByRevisedAtAsc(Long actionId);
}
