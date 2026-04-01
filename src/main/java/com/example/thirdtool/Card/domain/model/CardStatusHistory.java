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

    //Archive에 대한 기록 축적
    @Enumerated(EnumType.STRING)
    @Column(name = "reason", updatable = false, length = 20)
    private ArchiveReason reason;


    /** JPA 전용 기본 생성자. 외부에서 직접 사용 금지. */
    protected CardStatusHistory() {}

    private CardStatusHistory(Card card, CardStatus fromStatus, CardStatus toStatus, ArchiveReason reason) {
        this.card       = card;
        this.fromStatus = fromStatus;
        this.toStatus   = toStatus;
        this.reason     = reason;
    }

    static CardStatusHistory of(
            Card card,
            CardStatus fromStatus,
            CardStatus toStatus,
            ArchiveReason reason
                               ) {
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
                    "CardStatusHistory: fromStatus와 toStatus가 동일합니다. status=" + fromStatus);
        }

        // 방향별 reason 검증
        if (toStatus == CardStatus.ARCHIVE && reason == null) {
            throw CardDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "CardStatusHistory: ON_FIELD → ARCHIVE 이력에는 reason이 필수입니다.");
        }
        if (toStatus == CardStatus.ON_FIELD && reason != null) {
            throw CardDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "CardStatusHistory: ARCHIVE → ON_FIELD 복귀 이력에는 reason을 기록하지 않습니다. reason=" + reason);
        }

        return new CardStatusHistory(card, fromStatus, toStatus, reason);
    }
}
