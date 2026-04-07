package com.example.thirdtool.Card.domain.model;

import org.springframework.stereotype.Component;

import java.util.Optional;

//도메인 서비스
@Component
public class CardExpiryPolicy {

    public Optional<ArchiveReason> expire(Card card, OnFieldBudget budget) {
        Optional<ArchiveReason> reason = budget.resolveReason(card);
        reason.ifPresent(_ -> card.archive());
        return reason;
    }
}