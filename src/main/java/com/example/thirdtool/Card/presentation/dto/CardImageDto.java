package com.example.thirdtool.Card.presentation.dto;

import com.example.thirdtool.Card.domain.model.CardImage;
import com.example.thirdtool.Card.domain.model.ImageType;

public record CardImageDto(
        Long id,
        String imageUrl,
        ImageType imageType,
        Integer sequence
) {

    public static CardImageDto from(CardImage image) {
        return new CardImageDto(image.getId(),
                image.getImageUrl(),
                image.getImageType(),
                image.getSequence());
    }
}

