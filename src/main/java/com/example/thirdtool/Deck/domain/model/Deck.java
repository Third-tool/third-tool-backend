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
import java.util.Collections;
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

    // ✅ lastAccessed 필드 추가e
    private LocalDateTime lastAccessed;


    @Column(nullable = false)
    private boolean onLibrary = false;   // 공개 라이브러리 등록 여부

    private LocalDateTime publishedAt;   // 공개 시각 (정렬용)

    /** 덱 운영 모드. 기본값 ON_FIELD(활성 학습 모드). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeckMode mode = DeckMode.ON_FIELD;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_deck_id")
    private Deck parentDeck;

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

    // ─── LearningFacade 연결 (Story-005-1) ───────────────────
    // Long ID 참조 — BC 간 직접 객체 참조 회피 (docs/PACKAGE.md §6).
    // 자료 삭제 시 learning_material_id만 NULL로 전환 → "자료 미연결 Deck"으로 유지.
    // axis_id는 자료 삭제와 무관하게 영구 보존 (로드맵 추적성).

    @Column(name = "axis_id", nullable = true)
    private Long axisId;

    @Column(name = "learning_material_id", nullable = true)
    private Long learningMaterialId;

    // ─── 진행 상태 (Story-005-2) ─────────────────────────────
    // Card 추가/archive/returnToField 시점에 CardCommandService가 자동 갱신한다.
    // Card 도메인 자체가 다른 Aggregate(Deck) 상태를 변경하지 않음 — Service 조율.

    @Enumerated(EnumType.STRING)
    @Column(name = "progress_status", nullable = false, length = 20)
    private DeckProgressStatus progressStatus = DeckProgressStatus.NOT_STARTED;

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

    public static Deck of(String name, Deck parentDeck, UserEntity user) {
        validateName(name);
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Deck: user는 null일 수 없습니다.");
        }

        Deck deck = new Deck();
        deck.name         = name.trim();
        deck.parentDeck   = parentDeck;
        deck.user         = user;
        deck.depth        = (parentDeck == null) ? 0 : parentDeck.getDepth() + 1;
        deck.lastAccessed = LocalDateTime.now();
        deck.mode         = DeckMode.ON_FIELD;
        return deck;
    }

    /**
     * 학습 자료 등록 흐름에서 호출되는 정적 팩토리 (Story-005-1).
     * {@code LearningMaterialCreatedEvent} 핸들러가 사용한다.
     *
     * @param user      소유 사용자 (자료의 user와 동일)
     * @param axisId    자료가 연결된 축 ID (없으면 null — Deck은 어디 축에도 안 붙은 상태로 생성)
     * @param materialId 원천 자료 ID
     * @param name      Deck 이름 (자료명 또는 사용자가 요청한 별도 이름)
     */
    public static Deck createFromLearningMaterial(UserEntity user, Long axisId, Long materialId, String name) {
        validateName(name);
        if (user == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Deck: user는 null일 수 없습니다.");
        }
        if (materialId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Deck: materialId는 null일 수 없습니다.");
        }

        Deck deck = new Deck();
        deck.name               = name.trim();
        deck.user               = user;
        deck.axisId             = axisId;
        deck.learningMaterialId = materialId;
        deck.parentDeck         = null;
        deck.depth              = 0;
        deck.lastAccessed       = LocalDateTime.now();
        deck.mode               = DeckMode.ON_FIELD;
        return deck;
    }

    /**
     * 원천 학습 자료가 삭제되었을 때 호출 (Story-005-1).
     * Deck 자체는 유지하고 {@code learningMaterialId}만 null로 전환 — "자료 미연결 Deck".
     * {@code axisId}는 로드맵 추적성을 위해 보존한다.
     *
     * <p><strong>멱등성 보장</strong>: 이미 {@code learningMaterialId == null}이면 효과 없음 (예외 X).
     */
    public void markMaterialDeleted() {
        this.learningMaterialId = null;
    }

    // ─── 진행 상태 갱신 (Story-005-2) ─────────────────────────

    /**
     * NOT_STARTED → IN_PROGRESS 전용 마커. Deck에 첫 Card가 추가될 때 CardCommandService가 호출한다.
     *
     * <p><strong>멱등</strong>: 이미 IN_PROGRESS / COMPLETED이면 무시 — NOT_STARTED → IN_PROGRESS 전이만 수행.
     * COMPLETED → IN_PROGRESS 회귀는 본 메서드 책임 아님. {@link #recalculateProgressStatus()}가 담당.
     */
    public void markInProgress() {
        if (this.progressStatus == DeckProgressStatus.NOT_STARTED) {
            this.progressStatus = DeckProgressStatus.IN_PROGRESS;
        }
    }

    /**
     * Card archive / returnToField 등 cards 상태가 바뀐 후 호출 (Story-005-2).
     * soft delete된 Card는 제외하고 활성 Card만 기준:
     * <ul>
     *   <li>활성 Card 0개 → NOT_STARTED</li>
     *   <li>모든 활성 Card가 ARCHIVE → COMPLETED</li>
     *   <li>그 외 → IN_PROGRESS</li>
     * </ul>
     */
    public void recalculateProgressStatus() {
        List<Card> activeCards = this.cards.stream()
                .filter(card -> !card.isDeleted())
                .toList();

        if (activeCards.isEmpty()) {
            this.progressStatus = DeckProgressStatus.NOT_STARTED;
            return;
        }

        boolean allArchived = activeCards.stream().allMatch(Card::isArchived);
        this.progressStatus = allArchived ? DeckProgressStatus.COMPLETED : DeckProgressStatus.IN_PROGRESS;
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


    /**
     * 부모 덱을 변경하고 자신의 depth를 재계산한다.
     *
     * <p>⚠️ 하위 덱의 depth는 재귀적으로 갱신되지 않는다.
     * 호출자(DeckCommandService)가 {@code getSubDecks()}를 순회하며
     * 하위 덱 전체의 depth를 재귀 갱신할 책임을 가진다.
     */
    public void changeParent(Deck newParent) {
        this.parentDeck = newParent;
        this.depth      = (newParent == null) ? 0 : newParent.getDepth() + 1;
    }

    /**
     * 덱 운영 모드를 변경한다.
     */
    public void changeMode(DeckMode mode) {
        if (mode == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "Deck: mode는 null일 수 없습니다.");
        }
        this.mode = mode;
    }

    // 권한 검증용
    public boolean isOwner(Long userId) {
        return this.user.getId().equals(userId);
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
    // -------------------------------------------------------
    // 읽기 전용 뷰
    // -------------------------------------------------------

    /** 수정 불가능한 카드 목록 뷰를 반환한다. */
    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    /** 수정 불가능한 하위 덱 목록 뷰를 반환한다. */
    public List<Deck> getSubDecks() {
        return Collections.unmodifiableList(subDecks);
    }

    // -------------------------------------------------------
    // 내부 검증
    // -------------------------------------------------------

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(ErrorCode.DECK_NAME_BLANK);
        }
    }

}