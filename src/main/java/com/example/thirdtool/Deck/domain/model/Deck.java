package com.example.thirdtool.Deck.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "deck",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_deck_user_name",
                columnNames = {"user_id", "name"}
        )
)
@Entity
public class Deck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    // ✅ lastAccessed 필드 추가
    private LocalDateTime lastAccessed;

    // ✅ scoringAlgorithmType 필드 추가
    @Column(nullable = false, length = 50)
    private String scoringAlgorithmType;


    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_deck_id")
    private Deck parentDeck;

    // ✅ depth 추가- 자기참조 관련 관리 로직
    @Column(nullable = false)
    private int depth;


    @JsonIgnore
    @OneToMany(mappedBy = "parentDeck")
    private List<Deck> subDecks = new ArrayList<>();

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL)
    private List<Card> cards = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY) // ✅ User 엔티티와의 다대일 관계
    @JoinColumn(name = "user_id", nullable = false) // ✅ 외래 키 설정
    private UserEntity user;



    @Builder(builderMethodName = "internalBuilder")
    private Deck(String name, Deck parentDeck, String scoringAlgorithmType, UserEntity user) { // ✅ User 인자 추가
        this.name = name;
        this.parentDeck = parentDeck;
        this.lastAccessed = LocalDateTime.now();
        this.scoringAlgorithmType = scoringAlgorithmType;
        this.user = user; // ✅ user 필드 초기화
        // ✅ depth 계산
        if (parentDeck == null) {
            this.depth = 0; // root
        } else {
            this.depth = parentDeck.getDepth() + 1;
        }
    }

    public static Deck of(String name, Deck parentDeck, String scoringAlgorithmType, UserEntity user) { // ✅ User 인자 추가
        return internalBuilder()
                .name(name)
                .parentDeck(parentDeck)
                .scoringAlgorithmType(scoringAlgorithmType)
                .user(user) // ✅ user 전달
                .build();
    }

    public void updateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("덱 이름은 비어있을 수 없습니다.");
        }
        this.name = name;
    }

    public void updateLastAccessed() {
        this.lastAccessed = LocalDateTime.now();
    }

    // ✅ 부모 변경 시 depth도 다시 계산
    public void updateParent(Deck newParent) {
        this.parentDeck = newParent;
        this.depth = (newParent == null) ? 0 : newParent.getDepth() + 1;
    }

    public void changeParent(Deck newParent) {
        updateParent(newParent); // 기존 로직 재사용 (depth 재계산)
    }


}