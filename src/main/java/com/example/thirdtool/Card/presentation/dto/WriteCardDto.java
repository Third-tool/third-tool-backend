package com.example.thirdtool.Card.presentation.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record WriteCardDto(
        String question,
        String answer,
        List<MultipartFile> questionImages,  // 질문 이미지 여러 개
        List<MultipartFile> answerImages     // 답변 이미지 여러 개
) { }
