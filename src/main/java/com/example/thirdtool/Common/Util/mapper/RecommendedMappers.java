package com.example.thirdtool.Common.Util.mapper;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.presentation.dto.RecommendedCardDto;
import com.example.thirdtool.Common.Util.ThumbnailPolicy;
import lombok.experimental.UtilityClass;

// src/main/java/.../mapper/RecommendedMappers.java
@UtilityClass
public class RecommendedMappers {
    public RecommendedCardDto toRecommended(Card card) {
        String thumb = ThumbnailPolicy.pick(card); // ✅ 정책 적용
        // 필요 시 플레이스홀더로 강제 대체하려면 아래 주석 해제
        // if (thumb == null) thumb = ThumbnailPolicy.PLACEHOLDER;

        return RecommendedCardDto.of(
                card.getId(),
                card.getQuestion(),
                card.getAnswer(),
                thumb
                                    );
    }
}
