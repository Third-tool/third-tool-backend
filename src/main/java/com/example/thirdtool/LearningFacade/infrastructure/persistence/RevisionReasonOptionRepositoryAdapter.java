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
    public List<RevisionReasonOption> findAllByActiveTrueOrderByDisplayOrderAsc() {
        return jpa.findAllByActiveTrueOrderByDisplayOrderAsc();
    }

    @Override
    public Optional<RevisionReasonOption> findByLabelAndActiveTrue(String label) {
        return jpa.findByLabelAndActiveTrue(label);
    }

    @Override
    public Optional<RevisionReasonOption> findByIdAndActiveTrue(Long id) {
        return jpa.findByIdAndActiveTrue(id);
    }
}
