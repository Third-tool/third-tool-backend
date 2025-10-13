package com.example.thirdtool.Card.presentation.controller;

import com.example.thirdtool.Card.application.service.CardImageService;
import com.example.thirdtool.Card.application.service.CardService;
import com.example.thirdtool.Card.domain.model.ImageType;
import com.example.thirdtool.Card.presentation.dto.CardImageDto;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cards/{cardId}/images")
public class CardImageController {

    private final CardImageService cardImageService;


    // 임시 이미지 업로드 → URL 반환
    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(
            @PathVariable Long cardId,
            @RequestParam MultipartFile image,
            @RequestParam ImageType type,
            @RequestParam(defaultValue = "1") int sequence) {
        String url = cardImageService.uploadCardImage(cardId, image, type, sequence);
        return ResponseEntity.ok(url);
    }


    @GetMapping
    public ResponseEntity<List<CardImageDto>> getImages(@PathVariable Long cardId) {
        return ResponseEntity.ok(cardImageService.getCardImages(cardId));
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long cardId, @PathVariable Long imageId) {
        cardImageService.deleteCardImage(imageId);
        return ResponseEntity.noContent().build();
    }


}
