package com.example.thirdtool.Card.application.service;


import com.example.thirdtool.Card.Document.CardDocument;
import com.example.thirdtool.Card.domain.model.*;
import com.example.thirdtool.Card.domain.repository.CardRankRepository;
import com.example.thirdtool.Card.domain.repository.CardRepository;
import com.example.thirdtool.Card.presentation.dto.CardRankInfoDto;
import com.example.thirdtool.Card.presentation.dto.WriteCardDto;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
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
    private final CardSearchService cardSearchService;


    //카드 만들기
    @Transactional
    public Card createCard(Long deckId, WriteCardDto writeCardDto) {
        log.info("[CardService] 🧩 createCard 호출 - deckId={}, DTO={}", deckId, writeCardDto);

        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        String q = writeCardDto.getQuestion();
        String a = writeCardDto.getAnswer();
        log.info("[CardService] 🧠 DTO 값 확인 - question='{}', answer='{}'", q, a);

        if (q == null || a == null) {
            log.warn("[CardService] ⚠️ question 또는 answer가 null입니다. 클라이언트에서 전달 확인 필요!");
        }

        Card card = Card.of(q, a, deck);

        // ✅ 질문 이미지 여러 개 처리
        if (writeCardDto.getQuestionImages() != null && !writeCardDto.getQuestionImages().isEmpty()) {
            int sequence = 1;
            for (MultipartFile file : writeCardDto.getQuestionImages()) {
                if (file != null && !file.isEmpty()) {
                    String uploadedUrl = fileStoragePort.uploadFile(file, "question");
                    CardImage questionImage = CardImage.of(card, uploadedUrl, ImageType.QUESTION, sequence++);
                    card.addImage(questionImage);
                }
            }
        }

        // ✅ 답변 이미지 여러 개 처리
        if (writeCardDto.getAnswerImages() != null && !writeCardDto.getAnswerImages().isEmpty()) {
            int sequence = 1;
            for (MultipartFile file : writeCardDto.getAnswerImages()) {
                if (file != null && !file.isEmpty()) {
                    String uploadedUrl = fileStoragePort.uploadFile(file, "answer");
                    CardImage answerImage = CardImage.of(card, uploadedUrl, ImageType.ANSWER, sequence++);
                    card.addImage(answerImage);
                }
            }
        }

        // ✅ DB 저장
        Card savedCard = cardRepository.save(card);

        // ✅ Elasticsearch 인덱싱 (실패해도 트랜잭션 영향 X)
        try {
            CardDocument doc = CardDocument.from(savedCard);
            cardSearchService.indexCard(doc);
        } catch (Exception e) {
            log.error("[Elasticsearch] 인덱싱 실패 - cardId: {}, 이유: {}", savedCard.getId(), e.getMessage());
        }

        // 🔙 생성된 카드 반환
        return savedCard;
    }

    @Transactional
    public List<Card> createCards(Long deckId, List<WriteCardDto> dtos) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));

        List<Card> cards = dtos.stream()
                               .map(dto -> {
                                   Card card = Card.of(dto.getQuestion(), dto.getAnswer(), deck);

                                   if (dto.getQuestionImages() != null && !dto.getQuestionImages().isEmpty()) {
                                       int sequence = 1;
                                       for (MultipartFile file : dto.getQuestionImages()) {
                                           if (file != null && !file.isEmpty()) {
                                               String uploadedUrl = fileStoragePort.uploadFile(file, "question");
                                               CardImage questionImage = CardImage.of(card, uploadedUrl, ImageType.QUESTION, sequence++);
                                               card.addImage(questionImage);
                                           }
                                       }
                                   }

                                   if (dto.getAnswerImages() != null && !dto.getAnswerImages().isEmpty()) {
                                       int sequence = 1;
                                       for (MultipartFile file : dto.getAnswerImages()) {
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

        List<Card> savedCards = cardRepository.saveAll(cards);

        savedCards.forEach(card -> {
            try {
                cardSearchService.indexCard(CardDocument.from(card));
            } catch (Exception e) {
                log.error("[Elasticsearch] 일괄 인덱싱 실패 - cardId: {}, 이유: {}", card.getId(), e.getMessage());
            }
        });

        return savedCards;
    }

    // ✅ 카드 수정하기
    @Transactional
    public void updateCard(Long cardId, WriteCardDto dto) {
        Card card = cardRepository.findById(cardId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        // ✅ 텍스트 업데이트
        card.updateCard(dto.getQuestion(), dto.getAnswer());

        // ✅ 질문 이미지 여러 개 처리
        if (dto.getQuestionImages() != null && !dto.getQuestionImages().isEmpty()) {
            int sequence = 1;
            for (MultipartFile file : dto.getQuestionImages()) {
                if (file != null && !file.isEmpty()) {
                    String uploadedUrl = fileStoragePort.uploadFile(file, "question");
                    CardImage questionImage = CardImage.of(card, uploadedUrl, ImageType.QUESTION, sequence++);
                    card.addImage(questionImage);
                }
            }
        }

        // ✅ 답변 이미지 여러 개 처리
        if (dto.getAnswerImages() != null && !dto.getAnswerImages().isEmpty()) {
            int sequence = 1;
            for (MultipartFile file : dto.getAnswerImages()) {
                if (file != null && !file.isEmpty()) {
                    String uploadedUrl = fileStoragePort.uploadFile(file, "answer");
                    CardImage answerImage = CardImage.of(card, uploadedUrl, ImageType.ANSWER, sequence++);
                    card.addImage(answerImage);
                }
            }
        }

        // ✅ Elasticsearch 문서도 갱신
        try {
            cardSearchService.indexCard(CardDocument.from(card));
        } catch (Exception e) {
            log.error("[Elasticsearch] 문서 업데이트 실패 - cardId: {}, 이유: {}", card.getId(), e.getMessage());
        }
    }

    // ✅ 카드 삭제하기
    @Transactional
    public void deleteCard(Long cardId) {

        Card card = cardRepository.findById(cardId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
        // ✅ 실제 삭제 대신 updatedDate를 null로 설정
        card.markAsDeleted();

        // ✅ 카드에 연결된 이미지 S3 삭제
        card.getImages().forEach(image -> fileStoragePort.deleteFile(image.getImageUrl()));


        // elasticSearch도 동기화
        try {
            cardSearchService.deleteIndex(cardId);
        } catch (Exception e) {
            log.warn("[Elasticsearch] 인덱스 삭제 실패 - cardId: {}", cardId);
        }
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
    public Slice<CardRankInfoDto> getCardsByRank(Long userId,
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

        Slice<CardRankInfoDto> cards=cardRepository.findCardsByScoreRange(userId, deckId, mode, minScore, maxScore, pageable);

        return cards;
    }

    public Card getCardById(Long cardId) {
        return cardRepository.findById(cardId)
                             .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
    }

}