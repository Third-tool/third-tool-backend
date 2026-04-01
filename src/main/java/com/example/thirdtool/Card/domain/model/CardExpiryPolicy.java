package com.example.thirdtool.Card.domain.model;

import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CardExpiryPolicy {

    /**
     * 만료 판단과 사유 식별은 {@link OnFieldBudget#resolveReason(Card)}에 위임하고,
     * 만료된 카드의 상태 전환({@link Card#archive()})은 이 서비스가 수행한다.
     *
     * 이력 기록 조율은 Application Service가 책임진다.
     */
    public Optional<ArchiveReason> expire(Card card, OnFieldBudget budget) {
        Optional<ArchiveReason> reason = budget.resolveReason(card);
        reason.ifPresent(_ -> card.archive());
        return reason;
    }
}