package com.example.thirdtool.Card.domain.model;

import java.time.Duration;
import java.util.Optional;


//onField 체류 예산
public class OnFieldBudget {

    private final int      maxView;
    private final Duration maxDuration;

    private OnFieldBudget(int maxView, Duration maxDuration) {
        this.maxView     = maxView;
        this.maxDuration = maxDuration;
    }

    public static OnFieldBudget of(int maxView, Duration maxDuration) {
        if (maxView < 1) {
            throw new IllegalArgumentException("OnFieldBudget: maxView는 1 이상이어야 합니다. maxView=" + maxView);
        }
        if (maxDuration == null || maxDuration.isZero() || maxDuration.isNegative()) {
            throw new IllegalArgumentException("OnFieldBudget: maxDuration은 양수여야 합니다. maxDuration=" + maxDuration);
        }
        return new OnFieldBudget(maxView, maxDuration);
    }

    public Optional<ArchiveReason> resolveReason(Card card) {
        if (card.isArchived()) {
            return Optional.empty();
        }
        if (card.isMaxViewReached(maxView)) {
            return Optional.of(ArchiveReason.MAX_VIEW);
        }
        if (card.isDurationExceeded(maxDuration)) {
            return Optional.of(ArchiveReason.MAX_DURATION);
        }
        return Optional.empty();
    }

    public boolean isExpired(Card card) {
        return resolveReason(card).isPresent();
    }

    public int      getMaxView()     { return maxView; }
    public Duration getMaxDuration() { return maxDuration; }
}