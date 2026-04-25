package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.RevisionReasonOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RevisionReasonOptionJpaRepository extends JpaRepository<RevisionReasonOption, Long> {

    List<RevisionReasonOption> findAllByActiveTrueOrderByDisplayOrderAsc();

    Optional<RevisionReasonOption> findByLabelAndActiveTrue(String label);

    Optional<RevisionReasonOption> findByIdAndActiveTrue(Long id);
}
