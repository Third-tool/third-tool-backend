package com.example.thirdtool.Scoring.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.FeedbackType;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.Scoring.domain.model.algorithm.ScoringAlgorithm;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minidev.json.annotate.JsonIgnore;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "algorithm_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class LearningProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"}) // Hibernate Proxy 무시
    @OneToOne(mappedBy = "learningProfile")
    private Card card;

    protected int score;

    protected int greatCount;
    protected int goodCount;
    protected int normalCount;
    protected int badCount;

    @Enumerated(EnumType.STRING)
    private DeckMode mode;

    public static void initCommon(LearningProfile p) {
        p.score = 0;
        p.greatCount = p.goodCount = p.normalCount = p.badCount = 0;
        p.mode = DeckMode.THREE_DAY; // ★ 기본 모드
    }

    public int getScore() {
        return score;
    }

    // ✅ 공통 피드백 카운팅
    protected void incrementFeedback(FeedbackType feedback) {
        switch (feedback) {
            case GREAT -> greatCount++;
            case GOOD -> goodCount++;
            case NORMAL -> normalCount++;
            case BAD -> badCount++;
        }
    }

    // ✅ 알고리즘별 피드백 적용 책임은 자식이 가짐- 수정 가능성
    public abstract void applyFeedback(ScoringAlgorithm algorithm,
                                       Card card,
                                       FeedbackType feedback);

    // ✅ 공통 초기화
    public void reset(int newScore) {
        this.score = newScore;
        this.greatCount = 0;
        this.goodCount = 0;
        this.normalCount = 0;
        this.badCount = 0;
    }

    public void updateMode(DeckMode deckMode) {
        this.mode = deckMode;
    }

    public void ensureFeedbackAllowed() {
        if (this.mode != DeckMode.THREE_DAY) {
            throw new IllegalStateException("3DAY 모드에서만 학습 피드백을 진행할 수 있습니다.");
        }
    }

    public void linkToCard(Card card) {
        this.card = card;
    }
}