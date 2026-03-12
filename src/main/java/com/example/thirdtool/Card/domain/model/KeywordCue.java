package com.example.thirdtool.Card.domain.model;


import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import jakarta.persistence.*;

@Entity
@Table(name = "keyword_cue")
public class KeywordCue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "keyword_cue_id")
    private Long id;

    @Column(name = "value", nullable = false, length = 200)
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    /** JPA 전용 기본 생성자. 외부에서 직접 사용 금지. */
    protected KeywordCue() {}

    private KeywordCue(Card card, String value) {
        this.card  = card;
        this.value = value;
    }


    public static KeywordCue create(Card card, String value) {
        if (card == null) {
            // Card가 null인 것은 프로그래밍 오류이므로 INVALID_INPUT 사용
            throw CardDomainException.of(ErrorCode.INVALID_INPUT, "KeywordCue 생성 시 card는 null일 수 없습니다.");
        }
        String trimmed = trim(value);
        if (trimmed == null || trimmed.isBlank()) {
            throw CardDomainException.of(ErrorCode.CARD_KEYWORD_BLANK);
        }
        return new KeywordCue(card, trimmed);
    }

    public Long   getId()    { return id; }
    public String getValue() { return value; }
    public Card   getCard()  { return card; }

    /** 테스트 또는 영속성 복원 전용. */
    void setId(Long id) { this.id = id; }

    private static String trim(String v) { return v == null ? null : v.trim(); }

}
