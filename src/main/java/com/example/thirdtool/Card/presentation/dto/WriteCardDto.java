package com.example.thirdtool.Card.presentation.dto;

import org.springframework.web.multipart.MultipartFile;

public record WriteCardDto(
        String question,
        String answer,
        MultipartFile questionImage,  // 질문 이미지
        MultipartFile answerImage     // 답변 이미지
) {

}
