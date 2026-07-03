package com.example.thirdtool.Card.domain.model;


import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.UserSchedule.domain.model.LearningMode;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Entity
@Table(name = "card")
public class Card {

    // ─── 매직 넘버 ────────────────────────────────────────────────
    private static final int MAX_TAG_COUNT = 3;
    private static final LearningMode DEFAULT_CREATED_MODE = LearningMode.MODE_14D;

    // ─── 식별자 ──────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deck_id", nullable = false)
    private Deck deck;

    // ─── 축 직접 매핑 (Story-LT-E4-S4-3) ─────────────────────
    // Card가 소속 Axis를 직접 참조한다. 값은 create() 시점에 deck.getAxisId()로 유도.
    // M5 Deck 폐기에 대비한 사전 인프라 (deck.axis_id와 병존 · 데이터 정합 유지).
    // Raw Long 참조 — BC 간 직접 객체 참조 회피 (docs/PACKAGE.md §6).
    @Column(name = "axis_id", nullable = false)
    private Long axisId;

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
    @OneToMany(
            mappedBy      = "card",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    private final List<CardTag> cardTags = new ArrayList<>();

    // ─── 운영 위치 (CardStatus) ───────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status = CardStatus.ON_FIELD;

    // ─── ON_FIELD 체류 추적 ───────────────────────────────────
    @Column(name = "entered_field_at")
    private LocalDateTime enteredFieldAt;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    // ─── 생성 시점 학습 모드 (Story-CARD-E3-S3-2) ─────────────
    // 카드가 생성될 때 사용자의 현재 LearningMode를 스냅샷으로 보관한다.
    // 사용자가 나중에 모드를 down/up-shift해도 이 카드의 스케줄은 createdMode 기준으로 유지.
    // 하이브리드 로직: effectiveMaxDays(userCurrentMode) = min(createdMode.max, userCurrent.max)
    @Enumerated(EnumType.STRING)
    @Column(name = "created_mode", nullable = false, length = 10)
    private LearningMode createdMode;

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
    private Card(MainNote mainNote, Summary summary, LearningMode createdMode, LocalDate today) {
        this.mainNote       = mainNote;
        this.summary        = summary;
        this.status         = CardStatus.ON_FIELD;
        this.enteredFieldAt = today.atStartOfDay();
        this.viewCount      = 0;
        this.createdMode    = createdMode;
    }

    /**
     * Story-LT-E4-S4-3 — 카드의 축 참조 raw Long.
     * deck.getAxisId()와 동일값(정합 불변식). Deck 폐기 후에도 카드-축 소유권 유지.
     */
    public Long getAxisId() {
        return axisId;
    }

    // -------------------------------------------------------------------------
    // 생성
    // -------------------------------------------------------------------------

    /**
     * Story-CARD-E3-S3-4 — 확장된 팩토리. 사용자의 현재 mode(createdMode 스냅샷)와 오늘 날짜(enteredFieldAt 기준)를 주입받는다.
     */
    public static Card create(
            Deck deck,
            MainNote mainNote,
            Summary summary,
            List<String> keywordValues,
            List<Tag> tagList,
            LearningMode createdMode,
            LocalDate today
                             ) {
        requireNonNull(deck,          "deck");
        requireNonNull(mainNote,      "mainNote");
        requireNonNull(summary,       "summary");
        requireNonNull(keywordValues, "keywordValues");
        requireNonNull(createdMode,   "createdMode");
        requireNonNull(today,         "today");

        if (keywordValues.isEmpty()) {
            throw CardDomainException.of(ErrorCode.CARD_KEYWORD_MIN_REQUIRED);
        }

        List<Tag> resolvedTags = tagList == null ? Collections.emptyList() : tagList;

        if (resolvedTags.size() > MAX_TAG_COUNT) {
            throw CardDomainException.of(
                    ErrorCode.CARD_TAG_LIMIT_EXCEEDED,
                    "생성 시 태그는 최대 " + MAX_TAG_COUNT + "개까지 허용됩니다.");
        }

        Card card  = new Card(mainNote, summary, createdMode, today);
        card.deck  = deck;
        card.axisId = deck.getAxisId();  // Story-LT-E4-S4-3 — deck.axisId 스냅샷 (정합 불변식)
        keywordValues.forEach(v -> card.keywordCues.add(KeywordCue.create(card, v)));
        resolvedTags.forEach(tag -> card.cardTags.add(CardTag.link(card, tag)));
        return card;
    }

    /**
     * @deprecated Story-CARD-E3-S3-4 이관 후 호환 오버로드. Card.create(..., createdMode, today) 사용 권장.
     * default: createdMode = MODE_14D, today = LocalDate.now(). 다음 릴리스에서 제거 예정.
     */
    @Deprecated
    public static Card create(
            Deck deck,
            MainNote mainNote,
            Summary summary,
            List<String> keywordValues,
            List<Tag> tagList
                             ) {
        return create(deck, mainNote, summary, keywordValues, tagList, DEFAULT_CREATED_MODE, LocalDate.now());
    }

    /**
     * @deprecated Story-CARD-E3-S3-4 이관 후 호환 오버로드. 다음 릴리스에서 제거 예정.
     */
    @Deprecated
    public static Card create(
            Deck deck,
            MainNote mainNote,
            Summary summary,
            List<String> keywordValues
                             ) {
        return create(deck, mainNote, summary, keywordValues, null, DEFAULT_CREATED_MODE, LocalDate.now());
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
        if (keywordCues.size() <= 1) {
            throw CardDomainException.of(ErrorCode.CARD_KEYWORD_LAST_CANNOT_REMOVE);
        }
        KeywordCue target = keywordCues.stream()
                                       .filter(c -> java.util.Objects.equals(keywordCueId, c.getId()))
                                       .findFirst()
                                       .orElseThrow(() -> CardDomainException.of(
                                               ErrorCode.CARD_KEYWORD_NOT_FOUND,
                                               "keywordCueId=" + keywordCueId));
        keywordCues.remove(target);
    }

    // -------------------------------------------------------------------------
    // 태그
    // -------------------------------------------------------------------------

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

    /**
     * Story-CARD-E3-S3-3 — ARCHIVE → ON_FIELD fresh 재시작.
     *
     * <p>재학습 = 새로운 학습 계약 의도. `createdMode`를 사용자의 현재 모드로 재기록해
     * `effectiveMaxDays` 하이브리드 판정이 새로운 인터벌 계약을 반영하게 한다.
     *
     * @param userCurrentMode 사용자의 현재 학습 모드 (createdMode로 재기록)
     * @param today 오늘 날짜 (enteredFieldAt으로 재기록)
     */
    public void returnToField(LearningMode userCurrentMode, LocalDate today) {
        requireNonNull(userCurrentMode, "userCurrentMode");
        requireNonNull(today,           "today");
        if (this.status == CardStatus.ON_FIELD) return;
        this.status         = CardStatus.ON_FIELD;
        this.enteredFieldAt = today.atStartOfDay();
        this.viewCount      = 0;
        this.lastViewedAt   = null;
        this.createdMode    = userCurrentMode;
    }

    /**
     * @deprecated Story-CARD-E3-S3-3 이관 후 호환 오버로드. `returnToField(userCurrentMode, today)` 사용 권장.
     * default: userCurrentMode = MODE_14D, today = LocalDate.now(). 다음 릴리스에서 제거 예정.
     */
    @Deprecated
    public void returnToField() {
        returnToField(DEFAULT_CREATED_MODE, LocalDate.now());
    }

    public void recordView() {
        if (this.status == CardStatus.ARCHIVE) return;
        this.viewCount++;
        this.lastViewedAt = LocalDateTime.now();
    }

    public boolean isScheduleAvailable(Duration minInterval) {
        if (minInterval == null || minInterval.isZero() || minInterval.isNegative()) return true;
        if (this.lastViewedAt == null) return true;
        return Duration.between(this.lastViewedAt, LocalDateTime.now()).compareTo(minInterval) >= 0;
    }

    // -------------------------------------------------------------------------
    // M3 하이브리드 판정 (Story-CARD-E3-S3-2)
    //
    // 규칙:
    //   - effectiveMaxDays = min(createdMode.maxDays, userCurrentMode.maxDays)
    //     · down-shift(MODE_28D→MODE_7D): 카드의 createdMode(28)보다 사용자 현재(7)가 짧으면 즉시 cap
    //     · up-shift(MODE_7D→MODE_28D): 카드의 createdMode(7)가 유지 — 계약 확장 안 함
    //   - effectiveIntervals = createdMode.intervals 중 effectiveMaxDays 이하만
    //   - isDueOn = daysSinceEntered ∈ effectiveIntervals
    //   - hasScheduleExhausted = daysSinceEntered > effectiveMaxDays
    //
    // 시간대: LocalDate/enteredFieldAt은 서버 로컬(운영 프로필: KST) 기준.
    //         사용자별 시간대 지원은 v2 이관. daysSinceEntered는 KST 자정 경계로 계산.
    //
    // 소비처: M4에는 판정 소비처 없음 (dead code 상태). M5 DailyLearningBatch가 hasScheduleExhausted를
    //         소비해 archive 트리거. FE는 createdMode 노출값으로 사용자 안내에 활용.
    // -------------------------------------------------------------------------

    public int effectiveMaxDays(LearningMode userCurrentMode) {
        requireNonNull(userCurrentMode, "userCurrentMode");
        return Math.min(createdMode.maxDays(), userCurrentMode.maxDays());
    }

    public List<Integer> effectiveIntervals(LearningMode userCurrentMode) {
        int cap = effectiveMaxDays(userCurrentMode);
        return createdMode.getIntervals().stream()
                          .filter(day -> day <= cap)
                          .toList();
    }

    public boolean isDueOn(LocalDate today, LearningMode userCurrentMode) {
        requireNonNull(today, "today");
        int daysSince = daysSinceEntered(today);
        return effectiveIntervals(userCurrentMode).contains(daysSince);
    }

    public boolean hasScheduleExhausted(LearningMode userCurrentMode, LocalDate today) {
        requireNonNull(today, "today");
        return daysSinceEntered(today) > effectiveMaxDays(userCurrentMode);
    }

    private int daysSinceEntered(LocalDate today) {
        if (this.enteredFieldAt == null) return 0;
        long days = ChronoUnit.DAYS.between(this.enteredFieldAt.toLocalDate(), today);
        return (int) Math.max(0, days);
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
    public LearningMode  getCreatedMode()   { return createdMode; }

    public List<KeywordCue> getKeywordCues() {
        return Collections.unmodifiableList(keywordCues);
    }

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
