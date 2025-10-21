package com.example.thirdtool.Card.presentation.dto;

import com.example.thirdtool.Card.domain.model.CardImage;
import com.example.thirdtool.Card.domain.model.ImageType;

public record CardImageDto(
        Long id,
        String imageUrl,
        ImageType imageType,
        Integer sequence
) {

    //이미지 전달은 불변이 예상되므로 record 사용
    public static CardImageDto of(CardImage image) {
        return new CardImageDto(
                image.getId(),
                image.getImageUrl(),
                image.getImageType(),
                image.getSequence()
        );
    }

    public static CardImageDto from(CardImage image) {
        return new CardImageDto(image.getId(),
                image.getImageUrl(),
                image.getImageType(),
                image.getSequence());
    }
}

