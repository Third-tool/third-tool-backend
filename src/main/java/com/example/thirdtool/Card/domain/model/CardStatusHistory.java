package com.example.thirdtool.Card.domain.model;

import com.example.thirdtool.Card.domain.exception.CardDomainException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "card_status_history",
        indexes = {
                @Index(name = "idx_card_status_history_card_id",   columnList = "card_id"),
                @Index(name = "idx_card_status_history_changed_at", columnList = "changed_at")
        }
)
public class CardStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "card_status_history_id")
    private Long id;

    // Card 삭제 시 이력도 함께 제거된다. (ON DELETE CASCADE) 변경 이력 테이블이다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false, updatable = false)
    private Card card;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, updatable = false, length = 20)
    private CardStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, updatable = false, length = 20)
    private CardStatus toStatus;

    // 생성 시점에 자동 기록. 변경 불가.
    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    /** JPA 전용 기본 생성자. 외부에서 직접 사용 금지. */
    protected CardStatusHistory() {}

    private CardStatusHistory(Card card, CardStatus fromStatus, CardStatus toStatus) {
        this.card       = card;
        this.fromStatus = fromStatus;
        this.toStatus   = toStatus;
    }

    static CardStatusHistory of(Card card, CardStatus fromStatus, CardStatus toStatus) {
        if (card == null) {
            throw CardDomainException.of(
                    ErrorCode.INVALID_INPUT, "CardStatusHistory: card는 null일 수 없습니다.");
        }
        if (fromStatus == null) {
            throw CardDomainException.of(
                    ErrorCode.INVALID_INPUT, "CardStatusHistory: fromStatus는 null일 수 없습니다.");
        }
        if (toStatus == null) {
            throw CardDomainException.of(
                    ErrorCode.INVALID_INPUT, "CardStatusHistory: toStatus는 null일 수 없습니다.");
        }
        if (fromStatus == toStatus) {
            throw CardDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "CardStatusHistory: fromStatus와 toStatus가 동일합니다. 전환 이력은 상태가 달라야 합니다. status=" + fromStatus);
        }
        return new CardStatusHistory(card, fromStatus, toStatus);
    }
}
