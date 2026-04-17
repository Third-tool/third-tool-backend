package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.ActionRevision;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ActionRevisionRepositoryAdapter implements ActionRevisionRepository {

    private final ActionRevisionJpaRepository jpa;

    @Override
    public ActionRevision save(ActionRevision revision) {
        return jpa.save(revision);
    }

    @Override
    public List<ActionRevision> findByActionIdOrderByRevisedAtAsc(Long actionId) {
        return jpa.findByActionIdOrderByRevisedAtAsc(actionId);
    }
}
