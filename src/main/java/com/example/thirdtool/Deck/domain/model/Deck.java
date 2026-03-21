package com.example.thirdtool.Deck.domain.model;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigInteger;
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


    @Column(nullable = false)
    private boolean onLibrary = false;   // 공개 라이브러리 등록 여부

    private LocalDateTime publishedAt;   // 공개 시각 (정렬용)

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

    @OneToMany(mappedBy = "deck", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Card> cards = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY) // ✅ User 엔티티와의 다대일 관계
    @JoinColumn(name = "user_id", nullable = false) // ✅ 외래 키 설정
    private UserEntity user;

    // ─── Soft Delete ──────────────────────────────────────
    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;


    @Builder(builderMethodName = "internalBuilder")
    private Deck(String name, Deck parentDeck, UserEntity user) { // ✅ User 인자 추가
        this.name = name;
        this.parentDeck = parentDeck;
        this.lastAccessed = LocalDateTime.now();
        this.user = user; // ✅ user 필드 초기화
        this.depth = (parentDeck == null) ? 0 : parentDeck.getDepth() + 1;
    }

    public static Deck of(String name, Deck parentDeck, UserEntity user) { // ✅ User 인자 추가
        return internalBuilder()
                .name(name)
                .parentDeck(parentDeck)
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

    public void changeParent(Deck newParent) {
        this.parentDeck = newParent;
        this.depth      = (newParent == null) ? 0 : newParent.getDepth() + 1;
    }

    // ─── Soft Delete ──────────────────────────────────────

    /**
     * 덱 논리 삭제.
     * 덱에 속한 Card 전체도 연쇄 논리 삭제한다.
     */
    public void softDelete() {
        if (this.deleted) {
            throw new BusinessException(ErrorCode.DECK_ALREADY_DELETED);
        }
        this.deleted   = true;
        this.deletedAt = LocalDateTime.now();

        this.cards.forEach(Card::softDelete);
    }

    /**
     * 덱 복구.
     * 덱 삭제 시점과 동일하게 삭제된 Card만 함께 복구한다.
     * 덱 삭제 이전에 개별 삭제된 Card는 복구하지 않는다.
     */
    public void restore() {
        if (!this.deleted) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "삭제되지 않은 덱은 복구할 수 없습니다.");
        }
        LocalDateTime deckDeletedAt = this.deletedAt;

        this.deleted   = false;
        this.deletedAt = null;

        // deletedAt이 덱 삭제 시점 이후인 Card만 복구 (덱과 함께 삭제된 Card)
        this.cards.stream()
                  .filter(card -> card.isDeleted()
                          && card.getDeletedAt() != null
                          && !card.getDeletedAt().isBefore(deckDeletedAt))
                  .forEach(Card::restore);
    }

}