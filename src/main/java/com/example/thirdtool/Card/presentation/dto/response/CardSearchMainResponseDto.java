package com.example.thirdtool.Card.presentation.dto.response;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.presentation.dto.CardImageGroupDto;
import com.example.thirdtool.Common.Util.ThumbnailPolicy;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// ✅ 검색-진입 전용(덱 정보/썸네일 포함)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CardSearchMainResponseDto {
    private Long id;
    private String question;
    private String answer;

    private String thumbnailUrl;       // 썸네일 (정책 기반)
    private CardImageGroupDto images;  // 이미지 묶음

    private Long deckId;               // 덱 정보
    private String deckName;

    public static CardSearchMainResponseDto of(Card card, CardImageGroupDto images) {
        String thumb = ThumbnailPolicy.pick(card); // 🔁 정책 적용
        return CardSearchMainResponseDto.builder()
                                        .id(card.getId())
                                        .question(card.getQuestion())
                                        .answer(card.getAnswer())
                                        .thumbnailUrl(thumb)
                                        .images(images)
                                        .deckId(card.getDeck().getId())
                                        .deckName(card.getDeck().getName())
                                        .build();
    }

    // ⛔ 기존 CardMainResponseDto → from() 변환은 더 이상 필드가 맞지 않아 제거 권장
    // 필요하면 아래처럼 '이미지 묶음만' 가져와서 최소 변환만 수행하세요.
    public static CardSearchMainResponseDto from(Card card, CardMainResponseDto src) {
        String thumb = ThumbnailPolicy.pick(card);
        return CardSearchMainResponseDto.builder()
                                        .id(src.getId())
                                        .question(src.getQuestion())
                                        .answer(src.getAnswer())
                                        .thumbnailUrl(thumb)
                                        .images(src.getImages())
                                        .deckId(card.getDeck().getId())
                                        .deckName(card.getDeck().getName())
                                        .build();
    }
}
