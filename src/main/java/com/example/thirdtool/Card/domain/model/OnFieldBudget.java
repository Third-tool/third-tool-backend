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

    /**
     * ON_FIELD 체류 예산을 생성한다.
     *
     * @param maxView     최대 허용 노출 횟수 (1 이상)
     * @param maxDuration 최대 허용 체류 기간 (양수)
     * @throws IllegalArgumentException maxView가 1 미만이거나 maxDuration이 null 또는 0 이하이면
     */
    public static OnFieldBudget of(int maxView, Duration maxDuration) {
        if (maxView < 1) {
            throw new IllegalArgumentException("OnFieldBudget: maxView는 1 이상이어야 합니다. maxView=" + maxView);
        }
        if (maxDuration == null || maxDuration.isZero() || maxDuration.isNegative()) {
            throw new IllegalArgumentException("OnFieldBudget: maxDuration은 양수여야 합니다. maxDuration=" + maxDuration);
        }
        return new OnFieldBudget(maxView, maxDuration);
    }

    /**
     * 만료 조건을 판단하고 해당하는 {@link ArchiveReason}을 반환한다.
     * 카드 Archive 상태 - empty 반환
     * maxview 도달 - ArchiveReason 반환
     * maxDuration 도달 - ArchiveReason 반환
     * 없으면 - empty 반환
     */
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

    /**
     * 만료 여부만 필요한 호출자를 위한 편의 메서드.
     */
    public boolean isExpired(Card card) {
        return resolveReason(card).isPresent();
    }

    public int      getMaxView()     { return maxView; }
    public Duration getMaxDuration() { return maxDuration; }
}