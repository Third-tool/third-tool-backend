package com.example.thirdtool.Card.domain.model;


import com.example.thirdtool.Common.BaseEntity;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.Scoring.domain.model.ScoringAlgorithm;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Duration;
import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "cards")
@Entity
public class Card extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String question; // 질문

    @Column(nullable = false, length = 500)
    private String answer; // 답변

    @ColumnDefault("0")
    private int greatCount; // GREAT 선택 횟수
    @ColumnDefault("0")
    private int goodCount; // GOOD 선택 횟수
    @ColumnDefault("0")
    private int normalCount; // NORMAL 선택 횟수
    @ColumnDefault("0")
    private int badCount; // BAD 선택 횟수

    private int score; // SM-2의 난이도를 기반으로 하는 점수

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck; // Deck 엔티티와 다대일 관계 (FK)

    // ✅ SM-2 알고리즘을 위한 추가 필드
    private int repetition; // 반복 횟수
    private double easinessFactor; // 난이도 계수

    @Enumerated(EnumType.STRING)
    private DeckMode mode;

    @Builder(builderMethodName = "internalBuilder")
    private Card(String question, String answer, int greatCount, int goodCount,
                 int normalCount, int badCount, int score, Deck deck, int repetition, double easinessFactor, DeckMode mode) {
        this.question = question;
        this.answer = answer;
        this.greatCount = greatCount;
        this.goodCount = goodCount;
        this.normalCount = normalCount;
        this.badCount = badCount;
        this.score = score;
        this.deck = deck;
        this.repetition = repetition;
        this.easinessFactor = easinessFactor;
        this.mode = mode;
    }

    public static Card of(String question, String answer, Deck deck) {
        return internalBuilder()
                .question(question)
                .answer(answer)
                .greatCount(0)
                .goodCount(0)
                .normalCount(0)
                .badCount(0)
                .score(0)
                .deck(deck)
                .repetition(0)
                .easinessFactor(2.5)
                .mode(DeckMode.THREE_DAY)
                .build();
    }

    // ✅ 알고리즘을 받아 점수를 업데이트하는 퍼블릭 메서드 (엔티티가 로직 수행을 위임)
    public void updateScoreWithAlgorithm(ScoringAlgorithm algorithm, FeedbackType feedback) {
        algorithm.updateScore(this, feedback);
    }

    // ✅ Scoring 도메인에서 호출할 상태 변경 메서드 (캡슐화 유지)
    public void setScoreByAlgorithm(int newScore, int newRepetition, double newEasinessFactor,
                                    int newGreatCount, int newGoodCount, int newNormalCount, int newBadCount) {
        this.score = newScore;
        this.repetition = newRepetition;
        this.easinessFactor = newEasinessFactor;
        this.greatCount = newGreatCount;
        this.goodCount = newGoodCount;
        this.normalCount = newNormalCount;
        this.badCount = newBadCount;
    }


    public void updateMode(DeckMode deckMode) {
        this.mode = deckMode;
    }

    public void updateCard(String question, String answer) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("카드 질문은 비어있을 수 없습니다.");
        }
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("카드 답변은 비어있을 수 없습니다.");
        }
        this.question = question;
        this.answer = answer;
    }

    // ✅ 새로운 점수로 학습 지표를 재설정하는 메서드
    public void resetLearningMetrics(int newScore) {
        this.mode = DeckMode.THREE_DAY; // 모드를 3day로 변경
        this.score = newScore; // 원하는 점수로 설정
        this.easinessFactor = 2.5; // 난이도 계수 초기화
    }
}