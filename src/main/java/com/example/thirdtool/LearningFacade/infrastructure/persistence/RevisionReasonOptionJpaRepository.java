package com.example.thirdtool.LearningFacade.infrastructure.persistence;

public interface RevisionReasonOptionJpaRepository extends JpaRepository<RevisionReasonOption, Long> {

    List<RevisionReasonOption> findAllByActiveTrueOrderByDisplayOrderAsc();

    Optional<RevisionReasonOption> findByLabelAndActiveTrue(String label);

    Optional<RevisionReasonOption> findByIdAndActiveTrue(Long id);
}