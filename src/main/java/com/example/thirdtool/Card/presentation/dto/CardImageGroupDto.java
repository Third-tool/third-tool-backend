package com.example.thirdtool.Card.presentation.dto;

import com.example.thirdtool.Card.domain.model.ImageType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CardImageGroupDto {
    private List<CardImageDto> questionImages;
    private List<CardImageDto> answerImages;

    public static CardImageGroupDto from(List<CardImageDto> allImages) {
        List<CardImageDto> questionImages = allImages.stream()
                                                     .filter(img -> img.imageType() == ImageType.QUESTION)
                                                     .toList();

        List<CardImageDto> answerImages = allImages.stream()
                                                   .filter(img -> img.imageType() == ImageType.ANSWER)
                                                   .toList();

        return new CardImageGroupDto(questionImages, answerImages);
    }
}