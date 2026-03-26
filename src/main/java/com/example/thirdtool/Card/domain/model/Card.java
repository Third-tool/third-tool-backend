package com.example.thirdtool.Card.domain.model;


import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Entity
@Table(name = "card")
public class Card {
    // static 영역
    private static final int MAX_TAG_COUNT = 3;

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

    // ─── Soft Delete ─────────────────────────────────────────
    @Column(nullable = false)
    private boolean deleted = false;

    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    private LocalDateTime updatedDate;

    protected Card() {}

    private Card(Deck deck, MainNote mainNote, List<String> cueContents, Summary summary) {
        this.deck    = deck;
        this.mainNote = mainNote;
        this.summary  = summary;
        for (int i = 0; i < cueContents.size(); i++) {
            this.keywordCues.add(KeywordCue.create(this, cueContents.get(i)));
        }
    }

    /** deck 없음 — create() 내부 전용 */
    private Card(MainNote mainNote, Summary summary) {
        this.mainNote = mainNote;
        this.summary  = summary;
    }

    public static Card of(Deck deck, MainNote mainNote, List<String> cueContents, Summary summary) {
        if (deck == null) throw new IllegalArgumentException("Card: Deck은 필수입니다.");
        if (mainNote == null) throw new IllegalArgumentException("Card: MainNote는 필수입니다.");
        if (summary == null) throw new IllegalArgumentException("Card: Summary는 필수입니다.");
        return new Card(deck, mainNote, cueContents == null ? Collections.emptyList() : cueContents, summary);
    }

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
                    "생성 시 태그는 최대 " + MAX_TAG_COUNT + "개까지 허용됩니다."
                                        );
        }

        Card card = new Card(mainNote, summary);
        card.deck = deck;
        keywordValues.forEach(v -> card.keywordCues.add(KeywordCue.create(card, v)));
        resolvedTags.forEach(tag -> card.cardTags.add(CardTag.link(card,tag)));
        return card;
    }

    /**
     * 태그 없이 카드를 생성한다. (기존 코드 호환용)
     */
    public static Card create(
            Deck deck,
            MainNote mainNote,
            Summary summary,
            List<String> keywordValues
                             ) {
        return create(deck, mainNote, summary, keywordValues, null);
    }

    // -------------------------------------------------------------------------
    // 수정
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
    // 수정 — Tags
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
                    "현재 " + cardTags.size() + "개 연결됨. 최대 " + MAX_TAG_COUNT + "개까지 허용됩니다."
                                        );
        }
        boolean alreadyLinked = cardTags.stream()
                                        .anyMatch(ct -> ct.getTag().getId().equals(tag.getId()));
        if (alreadyLinked) {
            throw CardDomainException.of(
                    ErrorCode.CARD_TAG_ALREADY_EXISTS,
                    "tagId=" + tag.getId()
                                        );
        }
        cardTags.add(CardTag.link(this, tag));
    }

    /**
     * 태그를 단건 제거한다.
     * 연결되지 않은 tagId이면 예외를 발생시킨다.
     */
    public void removeTag(Long tagId) {
        CardTag target = cardTags.stream()
                                 .filter(ct -> ct.getTag().getId().equals(tagId))
                                 .findFirst()
                                 .orElseThrow(() -> CardDomainException.of(
                                         ErrorCode.CARD_TAG_NOT_FOUND,
                                         "tagId=" + tagId));
        cardTags.remove(target);
    }

    /**
     * 태그 목록을 전체 교체한다.
     * 4개 이상의 목록으로 교체할 수 없다.
     * null이면 빈 목록으로 교체한다.
     */
    public void replaceTags(List<Tag> newTags) {
        List<Tag> resolved = newTags == null ? Collections.emptyList() : newTags;
        if (resolved.size() > MAX_TAG_COUNT) {
            throw CardDomainException.of(
                    ErrorCode.CARD_TAG_LIMIT_EXCEEDED,
                    "교체 시 태그는 최대 " + MAX_TAG_COUNT + "개까지 허용됩니다."
                                        );
        }
        cardTags.clear();
        resolved.forEach(tag -> cardTags.add(CardTag.link(this, tag)));
    }


    // ─── Soft Delete ──────────────────────────────────────

    /**
     * 카드 논리 삭제.
     * 덱의 softDelete()에서 연쇄 호출되거나 개별 삭제 시 직접 호출된다.
     * 이미 삭제된 카드에 대한 중복 호출은 무시한다 (멱등성 보장).
     */
    public void softDelete() {
        if (this.deleted) return;   // 이미 삭제된 경우 무시 (멱등)
        this.deleted   = true;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 카드 복구.
     * Deck.restore()에서 덱 삭제 시점 기준으로 필터링된 Card에 대해서만 호출된다.
     */
    public void restore() {
        this.deleted   = false;
        this.deletedAt = null;
    }

    public Deck            getDeck()            { return deck; }
    public MainNote        getMainNote()        { return mainNote; }
    public Summary         getSummary()         { return summary; }
    public boolean         isDeleted()          { return deleted; }

    /** 수정 불가능한 뷰를 반환한다. Aggregate 외부에서 직접 컬렉션 조작 불가. */
    public List<KeywordCue> getKeywordCues() {
        return Collections.unmodifiableList(keywordCues);
    }


    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw CardDomainException.of(ErrorCode.INVALID_INPUT, fieldName + "은(는) null일 수 없습니다.");
        }
    }



}
