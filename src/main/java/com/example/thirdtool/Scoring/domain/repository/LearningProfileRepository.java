package com.example.thirdtool.Scoring.domain.repository;

import com.example.thirdtool.Scoring.domain.model.LearningProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface LearningProfileRepository extends JpaRepository<LearningProfile, Long> {
    @Query("select l from LearningProfile l where l.card.id = :cardId")
    Optional<LearningProfile> findByCardId(Long cardId);
}

