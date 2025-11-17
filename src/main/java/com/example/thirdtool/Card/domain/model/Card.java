package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Common.BaseEntity;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Scoring.domain.model.LearningProfile;
import com.example.thirdtool.Scoring.domain.model.LeitnerLearningProfile;
import com.example.thirdtool.Scoring.domain.model.Sm2LearningProfile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Card extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    private String question;

    @Lob
    private String answer;

    private boolean deleted = false;
    private LocalDateTime deletedAt;                // ✅ 삭제 시각 (optional)

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CardImage> images = new ArrayList<>();

    // ✅ Card 하나당 반드시 하나의 LearningState 보유
    @OneToOne(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private LearningProfile learningProfile;


    @Builder
    private Card(String question, String answer, Deck deck, LearningProfile learningProfile) {
        this.question = question;
        this.answer = answer;
        this.deck = deck;
        this.learningProfile = learningProfile;
    }


    // ✅ 정적 팩토리 메서드
    public static Card of(String question, String answer, Deck deck) {
        String algorithmType = deck.getScoringAlgorithmType().trim().toUpperCase();

        LearningProfile profile = switch (algorithmType) {
            case "SM2"     -> Sm2LearningProfile.init();
            case "LEITNER" -> LeitnerLearningProfile.init();
            default -> throw new IllegalArgumentException("지원하지 않는 알고리즘: " + algorithmType);
        };

        // 🔹 builder 시점부터 learningProfile 주입
        Card card = Card.builder()
                        .question(question)
                        .answer(answer)
                        .deck(deck)
                        .learningProfile(profile)
                        .build();

        profile.linkToCard(card);
        return card;
    }



    public void updateCard(String question, String answer) {
        if (question == null || question.isBlank()) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }
        if (answer == null || answer.isBlank()) {
            throw new BusinessException(ErrorCode.CARD_NOT_FOUND);
        }
        this.question = question;
        this.answer = answer;
    }


    // 내 점수 묻는 메서드
    public int currentScore() {
        return this.learningProfile.getScore();
    }

    // 이미지 업데이트
    public void updateImage(ImageType imageType, String newUrl, Integer sequence) {
        // 해당 타입의 기존 이미지 찾기
        Optional<CardImage> existingImage = this.images.stream()
                                                       .filter(img -> img.getImageType() == imageType && img.getSequence().equals(sequence))
                                                       .findFirst();

        if (existingImage.isPresent()) {
            existingImage.get().updateImage(newUrl, sequence);
        } else {
            // 없으면 새로 추가
            this.images.add(CardImage.of(this, newUrl, imageType, sequence));
        }
    }

    //사진 이미지 추가하기
    public void addImage(CardImage image) {
        this.images.add(image);
    }


    // ✅ 외부에서 알고리즘 타입을 가져올 때는 이 메서드만 쓰도록
    public String getScoringAlgorithmType() {
        if (deck == null) {
            throw new BusinessException(ErrorCode.DECK_NOT_FOUND);
        }
        return deck.getScoringAlgorithmType();
    }

    public void moveTo(Deck newDeck) {
        this.deck = newDeck;
    }

    public Card copyTo(Deck newDeck) {
        return Card.builder()
                   .question(this.question)
                   .answer(this.answer)
                   .deck(newDeck)
                   .learningProfile(null)
                   .build();
    }

    // ✅ Soft Delete 동작
    public void markAsDeleted() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }


}
