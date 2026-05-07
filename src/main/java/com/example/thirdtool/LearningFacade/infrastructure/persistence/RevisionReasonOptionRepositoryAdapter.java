package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.RevisionReasonOption;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RevisionReasonOptionRepositoryAdapter implements RevisionReasonOptionRepository {

    private final RevisionReasonOptionJpaRepository jpa;

    @Override
    public Optional<RevisionReasonOption> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public List<RevisionReasonOption> findActiveOrderByDisplayOrderAsc() {
        return jpa.findByActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    public Optional<RevisionReasonOption> findActiveById(Long id) {
        return jpa.findByIdAndActiveTrue(id);
    }
}
