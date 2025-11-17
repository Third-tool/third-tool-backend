package com.example.thirdtool.Card.application.resolver;

import com.example.thirdtool.Card.domain.model.CardRank;
import com.example.thirdtool.Card.domain.model.CardRankType;
import com.example.thirdtool.Card.domain.repository.CardRankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PermanentThresholdResolver {

    private final CardRankRepository cardRankRepository;

    /** 비즈니스 기본값(“고정” 아님, 초기 디폴트) */
    public static final int DEFAULT_THRESHOLD = 300;
    public static final int DEFAULT_SILVER_MIN = 0;

    @Transactional(readOnly = true)
    public int resolveForUser(Long userId) {
        return cardRankRepository.findByUserIdAndName(userId, CardRankType.DIAMOND.name())
                                 .map(CardRank::getMaxScore)
                                 .orElse(DEFAULT_THRESHOLD);
    }

    @Transactional(readOnly = true)
    public int resolveSilverMinForUser(Long userId) {
        return cardRankRepository.findByUserIdAndName(userId, CardRankType.SILVER.name())
                                 .map(CardRank::getMinScore)
                                 .orElse(DEFAULT_SILVER_MIN);
    }
}
