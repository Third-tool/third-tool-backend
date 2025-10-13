package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardImage;
import com.example.thirdtool.Card.domain.model.ImageType;
import com.example.thirdtool.Card.domain.repository.CardImageRepository;
import com.example.thirdtool.Card.domain.repository.CardRepository;
import com.example.thirdtool.Card.presentation.dto.CardImageDto;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.infra.adapter.FileStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CardImageService {

    private final CardRepository cardRepository;
    private final CardImageRepository cardImageRepository;
    private final FileStoragePort fileStoragePort;

    @Transactional
    public String uploadCardImage(Long cardId,
                                  MultipartFile file,
                                  ImageType type,
                                  int sequence) {
        // 1. 파일을 S3에 업로드
        String url = fileStoragePort.uploadFile(file, "cards/" + cardId);

        // 2. DB에 CardImage 엔티티 저장
        Card card = cardRepository.findById(cardId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        CardImage image = CardImage.of(card, url, type, sequence);
        cardImageRepository.save(image);

        return url;
    }


    @Transactional(readOnly = true)
    public List<CardImageDto> getCardImages(Long cardId) {

        return cardImageRepository.findByCardIdOrderBySequenceAsc(cardId).stream()
                                  .map(img -> new CardImageDto(img.getId(), img.getImageUrl(), img.getImageType(), img.getSequence()))
                                  .toList();
    }

    // 개별 이미지 삭제-s3에서 정리
    public void deleteCardImage(Long imageId) {
        CardImage image = cardImageRepository.findById(imageId)
                                             .orElseThrow(() -> new BusinessException(ErrorCode.CARD_IMAGE_NOT_FOUND));

        // ✅ S3 삭제
        fileStoragePort.deleteFile(image.getImageUrl());

        // ✅ DB 삭제
        cardImageRepository.delete(image);
    }



}


