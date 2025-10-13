package com.example.thirdtool.DailyLearningProgress.domain;

import com.example.thirdtool.Card.domain.model.CardRankType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "daily_learning_progress")
public class DailyLearningProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // 어떤 유저의 데이터인지 표시

    private int silverCount;
    private int goldCount;
    private int diamondCount;

    public static DailyLearningProgress init(Long userId) {
        return DailyLearningProgress.builder()
                                    .userId(userId)
                                    .silverCount(0)
                                    .goldCount(0)
                                    .diamondCount(0)
                                    .build();
    }

    @Builder
    private DailyLearningProgress(Long userId, int silverCount, int goldCount, int diamondCount) {
        this.userId = userId;
        this.silverCount = silverCount;
        this.goldCount = goldCount;
        this.diamondCount = diamondCount;
    }

    public void increment(CardRankType rankType) {
        switch (rankType) {
            case SILVER -> silverCount++;
            case GOLD -> goldCount++;
            case DIAMOND -> diamondCount++;
        }
    }

    public void reset() {
        silverCount = 0;
        goldCount = 0;
        diamondCount = 0;
    }
}