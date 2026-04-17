package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.RevisionReasonOption;

import java.util.List;
import java.util.Optional;

public interface RevisionReasonOptionRepository {

    List<RevisionReasonOption> findAllByActiveTrueOrderByDisplayOrderAsc();

    Optional<RevisionReasonOption> findByLabelAndActiveTrue(String label);

    Optional<RevisionReasonOption> findByIdAndActiveTrue(Long id);
}