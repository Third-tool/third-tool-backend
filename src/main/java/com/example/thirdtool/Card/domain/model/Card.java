package com.example.thirdtool.Card.domain.model;


import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Entity
@Table(name = "card")
public class Card {

    // ─── 매직 넘버 ────────────────────────────────────────────────
    private static final int MAX_TAG_COUNT = 3;

    // ─── 식별자 ──────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    // ─── 학습 맥락 (Main Notes) ──────────────────────────────
    @Embedded
    private MainNote mainNote;

    // ─── 핵심 압축 (Summary) ─────────────────────────────────
    @Embedded
    private Summary summary;

    // ─── 회상 단서 (Keyword / Cue) ───────────────────────────
    @OneToMany(
            mappedBy      = "card",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    private final List<KeywordCue> keywordCues = new ArrayList<>();

    // ─── 연결 태그 (CardTag) ──────────────────────────────────
    // @ManyToMany 대신 명시적 중간 엔티티로 관리한다.
    // 이유: linkedAt 보존, 역방향 조회 효율화, 태그 통계 확장 가능성
    @OneToMany(
            mappedBy      = "card",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    private final List<CardTag> cardTags = new ArrayList<>();

    // ─── 운영 위치 (CardStatus) ───────────────────────────────
    // 학습 성공/실패 평가가 아닌 "지금 어디에 위치해야 하는가"를 나타낸다.
    // 이력은 CardStatusHistory가 담당한다. Card는 현재 위치만 책임진다.
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status = CardStatus.ON_FIELD;

    // ─── ON_FIELD 체류 추적 ───────────────────────────────────
    // enteredFieldAt: 현재 ON_FIELD 구간 진입 시각.
    //   - 생성 시 현재 시각으로 초기화.
    //   - ARCHIVE → ON_FIELD 복귀 시 재기록.
    //   - archive() 시 보존. (마지막 ON_FIELD 구간 통계 근거 유지)
    @Column(name = "entered_field_at")
    private LocalDateTime enteredFieldAt;

    // viewCount: 현재 ON_FIELD 구간에서의 리뷰 세션 노출 횟수.
    //   - 생성 시 0으로 초기화.
    //   - ON_FIELD 복귀 시 0으로 초기화.
    //   - incrementViewCount() 호출로만 증가.
    //   - archive() 시 보존.
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    // ─── Soft Delete ─────────────────────────────────────────
    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    @Column(name = "last_viewed_at")
    private LocalDateTime lastViewedAt;

    /** JPA 전용 기본 생성자. 외부에서 직접 사용 금지. */
    protected Card() {}

    /** create() 내부 전용 생성자. */
    private Card(MainNote mainNote, Summary summary) {
        this.mainNote       = mainNote;
        this.summary        = summary;
        this.status         = CardStatus.ON_FIELD;
        this.enteredFieldAt = LocalDateTime.now();
        this.viewCount      = 0;
    }

    // -------------------------------------------------------------------------
    // 생성
    // -------------------------------------------------------------------------

    public static Card create(
            Deck deck,
            MainNote mainNote,
            Summary summary,
            List<String> keywordValues,
            List<Tag> tagList
                             ) {
        requireNonNull(deck,          "deck");
        requireNonNull(mainNote,      "mainNote");
        requireNonNull(summary,       "summary");
        requireNonNull(keywordValues, "keywordValues");

        if (keywordValues.isEmpty()) {
            throw CardDomainException.of(ErrorCode.CARD_KEYWORD_MIN_REQUIRED);
        }

        List<Tag> resolvedTags = tagList == null ? Collections.emptyList() : tagList;

        if (resolvedTags.size() > MAX_TAG_COUNT) {
            throw CardDomainException.of(
                    ErrorCode.CARD_TAG_LIMIT_EXCEEDED,
                    "생성 시 태그는 최대 " + MAX_TAG_COUNT + "개까지 허용됩니다.");
        }

        Card card  = new Card(mainNote, summary);
        card.deck  = deck;
        keywordValues.forEach(v -> card.keywordCues.add(KeywordCue.create(card, v)));
        resolvedTags.forEach(tag -> card.cardTags.add(CardTag.link(card, tag)));
        return card;
    }


    public static Card create(
            Deck deck,
            MainNote mainNote,
            Summary summary,
            List<String> keywordValues
                             ) {
        return create(deck, mainNote, summary, keywordValues, null);
    }

    // -------------------------------------------------------------------------
    // 수정 — 학습 구조
    // -------------------------------------------------------------------------

    public void changeMainNote(String textContent, String imageUrl) {
        this.mainNote = MainNote.of(textContent, imageUrl);
    }

    public void changeSummary(String value) {
        this.summary = Summary.of(value);
    }

    public void replaceKeywords(List<String> values) {
        requireNonNull(values, "values");
        if (values.isEmpty()) {
            throw CardDomainException.of(ErrorCode.CARD_KEYWORD_MIN_REQUIRED);
        }
        this.keywordCues.clear();
        values.forEach(v -> this.keywordCues.add(KeywordCue.create(this, v)));
    }

    public void addKeyword(String value) {
        this.keywordCues.add(KeywordCue.create(this, value));
    }

    public void removeKeyword(Long keywordCueId) {
        KeywordCue target = keywordCues.stream()
                                       .filter(c -> keywordCueId.equals(c.getId()))
                                       .findFirst()
                                       .orElseThrow(() -> CardDomainException.of(
                                               ErrorCode.CARD_KEYWORD_NOT_FOUND,
                                               "keywordCueId=" + keywordCueId));
        if (keywordCues.size() <= 1) {
            throw CardDomainException.of(ErrorCode.CARD_KEYWORD_LAST_CANNOT_REMOVE);
        }
        keywordCues.remove(target);
    }

    // -------------------------------------------------------------------------
    // 태그
    // -------------------------------------------------------------------------

    /**
     * 태그를 단건 추가한다.
     * 이미 3개이면 예외를 발생시킨다.
     * 이미 연결된 태그를 다시 추가하면 예외를 발생시킨다.
     */
    public void addTag(Tag tag) {
        requireNonNull(tag, "tag");
        if (cardTags.size() >= MAX_TAG_COUNT) {
            throw CardDomainException.of(
                    ErrorCode.CARD_TAG_LIMIT_EXCEEDED,
                    "현재 " + cardTags.size() + "개 연결됨. 최대 " + MAX_TAG_COUNT + "개까지 허용됩니다.");
        }
        boolean alreadyLinked = cardTags.stream()
                                        .anyMatch(ct -> ct.getTag().getId().equals(tag.getId()));
        if (alreadyLinked) {
            throw CardDomainException.of(ErrorCode.CARD_TAG_ALREADY_EXISTS, "tagId=" + tag.getId());
        }
        cardTags.add(CardTag.link(this, tag));
    }

    public void removeTag(Long tagId) {
        CardTag target = cardTags.stream()
                                 .filter(ct -> ct.getTag().getId().equals(tagId))
                                 .findFirst()
                                 .orElseThrow(() -> CardDomainException.of(
                                         ErrorCode.CARD_TAG_NOT_FOUND, "tagId=" + tagId));
        cardTags.remove(target);
    }

    public void replaceTags(List<Tag> newTags) {
        List<Tag> resolved = newTags == null ? Collections.emptyList() : newTags;
        if (resolved.size() > MAX_TAG_COUNT) {
            throw CardDomainException.of(
                    ErrorCode.CARD_TAG_LIMIT_EXCEEDED,
                    "교체 시 태그는 최대 " + MAX_TAG_COUNT + "개까지 허용됩니다.");
        }
        cardTags.clear();
        resolved.forEach(tag -> cardTags.add(CardTag.link(this, tag)));
    }

    // -------------------------------------------------------------------------
    // 운영 위치 전환 (CardStatus)
    // -------------------------------------------------------------------------

    public void archive() {
        if (this.status == CardStatus.ARCHIVE) return;
        this.status = CardStatus.ARCHIVE;
    }

    public void returnToField() {
        if (this.status == CardStatus.ON_FIELD) return;
        this.status         = CardStatus.ON_FIELD;
        this.enteredFieldAt = LocalDateTime.now();  // 새 ON_FIELD 구간 진입 시각 재기록
        this.viewCount      = 0;                    // 새 구간 노출 횟수 초기화
        this.lastViewedAt   = null;                 // 이전 구간 열람 시각은 schedule 판단 대상 아님
    }

    public void recordView() {
        if (this.status == CardStatus.ARCHIVE) return;
        this.viewCount++;
        this.lastViewedAt = LocalDateTime.now();
    }

    public boolean isMaxViewReached(int maxView) {
        if (maxView <= 0) return false;
        return this.viewCount >= maxView;
    }

    public boolean isDurationExceeded(Duration maxDuration) {
        if (this.enteredFieldAt == null) return false;
        return Duration.between(this.enteredFieldAt, LocalDateTime.now()).compareTo(maxDuration) >= 0;
    }

    //
    public boolean isLastView(int maxView) {
        if (maxView <= 0) return false;
        return this.viewCount == maxView;
    }

    public boolean isScheduleAvailable(Duration minInterval) {
        if (minInterval == null || minInterval.isZero() || minInterval.isNegative()) return true;
        if (this.lastViewedAt == null) return true;
        return Duration.between(this.lastViewedAt, LocalDateTime.now()).compareTo(minInterval) >= 0;
    }

    // -------------------------------------------------------------------------
    // 상태 조회
    // -------------------------------------------------------------------------

    public boolean isOnField() {
        return this.status == CardStatus.ON_FIELD;
    }

    public boolean isArchived() {
        return this.status == CardStatus.ARCHIVE;
    }

    // -------------------------------------------------------------------------
    // Soft Delete
    // -------------------------------------------------------------------------
    public void softDelete() {
        if (this.deleted) return;
        this.deleted   = true;
        this.deletedAt = LocalDateTime.now();
    }

    public void restore() {
        this.deleted   = false;
        this.deletedAt = null;
    }

    // -------------------------------------------------------------------------
    // 조회
    // -------------------------------------------------------------------------

    public Deck          getDeck()          { return deck; }
    public MainNote      getMainNote()      { return mainNote; }
    public Summary       getSummary()       { return summary; }
    public boolean       isDeleted()        { return deleted; }
    public LocalDateTime getEnteredFieldAt(){ return enteredFieldAt; }
    public int           getViewCount()     { return viewCount; }

    /** 수정 불가능한 뷰를 반환한다. Aggregate 외부에서 직접 컬렉션 조작 불가. */
    public List<KeywordCue> getKeywordCues() {
        return Collections.unmodifiableList(keywordCues);
    }

    /** 수정 불가능한 뷰를 반환한다. Aggregate 외부에서 직접 컬렉션 조작 불가. */
    public List<CardTag> getCardTags() {
        return Collections.unmodifiableList(cardTags);
    }

    // -------------------------------------------------------------------------
    // 내부 유틸
    // -------------------------------------------------------------------------

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw CardDomainException.of(
                    ErrorCode.INVALID_INPUT, fieldName + "은(는) null일 수 없습니다.");
        }
    }
}
