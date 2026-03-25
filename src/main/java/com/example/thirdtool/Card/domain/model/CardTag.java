package com.example.thirdtool.Card.domain.model;


import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Card와 Tag 사이의 연결을 명시적으로 관리하는 중간 엔티티.
 *
 * @ManyToMany + @JoinTable 대신 명시적 엔티티로 관리하는 이유:
 * - linkedAt(연결 시각) 같은 추가 컬럼 보존 가능
 * - CardRelationFinder의 태그 역방향 조회 시 직접 쿼리 가능
 * - 태그 사용 통계(1 Tag에 연결된 Card 수) 추후 확장 가능
 *
 * CardTag는 Card.addTag() / Card.removeTag()를 통해서만 생성·삭제된다.
 * 외부에서 직접 생성하지 않는다.
 */
@Getter
@Entity
@Table(
        name = "card_tag",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_card_tag",
                columnNames = {"card_id", "tag_id"}
        )
)
public class CardTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_tag_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    // 연결 시점에 자동 기록. 변경 불가.
    @Column(name = "linked_at", nullable = false, updatable = false)
    private LocalDateTime linkedAt;

    /** JPA 전용 기본 생성자. 외부에서 직접 사용 금지. */
    protected CardTag() {}

    private CardTag(Card card, Tag tag) {
        this.card     = card;
        this.tag      = tag;
        this.linkedAt = LocalDateTime.now();
    }

    /**
     * Card.addTag() 내부에서만 호출한다.
     * 외부에서 직접 생성하지 않는다.
     */
    static CardTag link(Card card, Tag tag) {
        if (card == null) {
            throw CardDomainException.of(ErrorCode.INVALID_INPUT, "CardTag 생성 시 card는 null일 수 없습니다.");
        }
        if (tag == null) {
            throw CardDomainException.of(ErrorCode.INVALID_INPUT, "CardTag 생성 시 tag는 null일 수 없습니다.");
        }
        return new CardTag(card, tag);
    }
}