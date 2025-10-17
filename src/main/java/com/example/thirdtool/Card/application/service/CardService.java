package com.example.thirdtool.Card.application.service;


import com.example.thirdtool.Card.domain.model.*;
import com.example.thirdtool.Card.domain.repository.CardRankRepository;
import com.example.thirdtool.Card.domain.repository.CardRepository;
import com.example.thirdtool.Card.presentation.dto.CardInfoDto;
import com.example.thirdtool.Card.presentation.dto.WriteCardDto;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import com.example.thirdtool.infra.S3.S3StorageAdapter;
import com.example.thirdtool.infra.adapter.FileStoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RequiredArgsConstructor
@Slf4j
@Service
public class CardService {

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final FileStoragePort fileStoragePort;
    private final CardRankRepository cardRankRepository;


    //카드 만들기
    @Transactional
    public void createCard(Long deckId, WriteCardDto writeCardDto) {

        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        Card card = Card.of(
                writeCardDto.question(),
                writeCardDto.answer(),
                deck);

        // ✅ 질문 이미지 여러 개 처리
        if (writeCardDto.questionImages() != null && !writeCardDto.questionImages().isEmpty()) {
            int sequence = 1;

            for (MultipartFile file : writeCardDto.questionImages()) {
                if (file != null && !file.isEmpty()) {
                    String uploadedUrl = fileStoragePort.uploadFile(file, "question");
                    CardImage questionImage = CardImage.of(card, uploadedUrl, ImageType.QUESTION, sequence++);
                    card.addImage(questionImage);
                }
            }
        }

        // ✅ 답변 이미지 여러 개 처리
        if (writeCardDto.answerImages() != null && !writeCardDto.answerImages().isEmpty()) {
            int sequence = 1;
            for (MultipartFile file : writeCardDto.answerImages()) {
                if (file != null && !file.isEmpty()) {
                    String uploadedUrl = fileStoragePort.uploadFile(file, "answer");
                    CardImage answerImage = CardImage.of(card, uploadedUrl, ImageType.ANSWER, sequence++);
                    card.addImage(answerImage);
                }
            }
        }
        // card 객체에 만들어서 한번에 다 집어넣었다.
        cardRepository.save(card);
    }

    @Transactional
    public void createCards(Long deckId, List<WriteCardDto> dtos) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        List<Card> cards = dtos.stream()
                               .map(dto -> {
                                   Card card = Card.of(dto.question(), dto.answer(), deck);

                                   // ✅ 질문 이미지 여러 개 처리
                                   if (dto.questionImages() != null && !dto.questionImages().isEmpty()) {
                                       int sequence = 1;
                                       for (MultipartFile file : dto.questionImages()) {
                                           if (file != null && !file.isEmpty()) {
                                               String uploadedUrl = fileStoragePort.uploadFile(file, "question");
                                               CardImage questionImage = CardImage.of(card, uploadedUrl, ImageType.QUESTION, sequence++);
                                               card.addImage(questionImage);
                                           }
                                       }
                                   }

                                   // ✅ 답변 이미지 여러 개 처리
                                   if (dto.answerImages() != null && !dto.answerImages().isEmpty()) {
                                       int sequence = 1;
                                       for (MultipartFile file : dto.answerImages()) {
                                           if (file != null && !file.isEmpty()) {
                                               String uploadedUrl = fileStoragePort.uploadFile(file, "answer");
                                               CardImage answerImage = CardImage.of(card, uploadedUrl, ImageType.ANSWER, sequence++);
                                               card.addImage(answerImage);
                                           }
                                       }
                                   }

                                   return card;
                               })
                               .toList();

        cardRepository.saveAll(cards);
    }

    // ✅ 카드 수정하기
    @Transactional
    public void updateCard(Long cardId, WriteCardDto dto) {
        Card card = cardRepository.findById(cardId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        // ✅ 텍스트 업데이트
        card.updateCard(dto.question(), dto.answer());

        // ✅ 질문 이미지 여러 개 처리
        if (dto.questionImages() != null && !dto.questionImages().isEmpty()) {
            int sequence = 1;
            for (MultipartFile file : dto.questionImages()) {
                if (file != null && !file.isEmpty()) {
                    String uploadedUrl = fileStoragePort.uploadFile(file, "question");
                    CardImage questionImage = CardImage.of(card, uploadedUrl, ImageType.QUESTION, sequence++);
                    card.addImage(questionImage);
                }
            }
        }

        // ✅ 답변 이미지 여러 개 처리
        if (dto.answerImages() != null && !dto.answerImages().isEmpty()) {
            int sequence = 1;
            for (MultipartFile file : dto.answerImages()) {
                if (file != null && !file.isEmpty()) {
                    String uploadedUrl = fileStoragePort.uploadFile(file, "answer");
                    CardImage answerImage = CardImage.of(card, uploadedUrl, ImageType.ANSWER, sequence++);
                    card.addImage(answerImage);
                }
            }
        }
    }

    // ✅ 카드 삭제하기
    @Transactional
    public void deleteCard(Long cardId) {

        Card card = cardRepository.findById(cardId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        // ✅ 카드에 연결된 이미지 S3 삭제
        card.getImages().forEach(image -> fileStoragePort.deleteFile(image.getImageUrl()));

        // ✅ Cascade + orphanRemoval 로 DB 정리
        cardRepository.delete(card);
    }



    // ✅ 무한 스크롤링 기반 카드 조회
    @Transactional
    public Slice<Card> getCardsByDeckId(Long deckId, int page, int size) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));
        deck.updateLastAccessed();

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        return cardRepository.findByDeckId(deckId, pageable);
    }

    @Transactional
    //무한 스크롤링 기반
    public Slice<Card> getCardsByDeckIdAndMode(Long deckId,
                                               DeckMode mode,
                                               int page,
                                               int size) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));
        deck.updateLastAccessed(); // ✅ 덱 입장할 때 자동 업데이트

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());

        return cardRepository.findByDeckIdAndProfileMode(deckId, mode, pageable);
    }


    @Transactional(readOnly = true)
    public Slice<CardInfoDto> getCardsByRank(Long userId,
                                             Long deckId,
                                             DeckMode mode,
                                             CardRankType rankName,
                                             int page,
                                             int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        // ✅ 1️⃣ 유저의 랭크 범위 조회
        CardRank rank = cardRankRepository.findByUserIdAndName(userId, rankName.name())
                                          .orElseThrow(() -> new BusinessException(ErrorCode.RANK_NOT_FOUND));
        int minScore = rank.getMinScore();
        int maxScore = rank.getMaxScore();

        Slice<CardInfoDto> cards=cardRepository.findCardsByScoreRange(userId, deckId, mode, minScore, maxScore, pageable);

        return cards;
    }

    public Card getCardById(Long cardId) {
        return cardRepository.findById(cardId)
                             .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
    }

}