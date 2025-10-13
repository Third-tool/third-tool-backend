package com.example.thirdtool.DailyLearningProgress.domain;

import com.example.thirdtool.Deck.domain.model.DeckMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
@Repository
public interface DailyLearningProgressRepository extends JpaRepository<DailyLearningProgress, Long> {

    Optional<DailyLearningProgress> findByUserId(Long userId);

    @Modifying
    @Query("UPDATE DailyLearningProgress d SET d.silverCount = 0, d.goldCount = 0, d.diamondCount = 0")
    void resetAllProgress();
}