package com.example.thirdtool.Deck.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Tag.domain.model.Tag;
import com.example.thirdtool.User.domain.model.User;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Deck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
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

    @JsonIgnore
    @OneToMany(mappedBy = "parentDeck")
    private List<Deck> subDecks = new ArrayList<>();

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL)
    private List<Card> cards = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY) // ✅ User 엔티티와의 다대일 관계
    @JoinColumn(name = "user_id", nullable = false) // ✅ 외래 키 설정
    private User user;

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(  //다대다 관계로 하나의 덱이 여러 태그를 가질 수 있다
        name = "deck_tag",  //중간 테이블 이름
        joinColumns = @JoinColumn(name = "deck_id"), //덱 테이블 참조하는 외래 키
        inverseJoinColumns = @JoinColumn(name = "tag_id") // 태그 테이블 참조하는 외래 키
    )

    private List<Tag> tags = new ArrayList<>();  //태그 리스트


    @Builder(builderMethodName = "internalBuilder")
    private Deck(String name, Deck parentDeck, String scoringAlgorithmType, User user) { // ✅ User 인자 추가
        this.name = name;
        this.parentDeck = parentDeck;
        this.lastAccessed = LocalDateTime.now();
        this.scoringAlgorithmType = scoringAlgorithmType;
        this.user = user; // ✅ user 필드 초기화
    }

    public static Deck of(String name, Deck parentDeck, String scoringAlgorithmType, User user) { // ✅ User 인자 추가
        return internalBuilder()
            .name(name)
            .parentDeck(parentDeck)
            .scoringAlgorithmType(scoringAlgorithmType)
            .user(user) // ✅ user 전달
            .build();
    }

    public static Deck of(String name, String scoringAlgorithmType, User user) { // ✅ User 인자 추가
        return internalBuilder()
            .name(name)
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

    public void setTags(List<Tag> tags) {
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }
}