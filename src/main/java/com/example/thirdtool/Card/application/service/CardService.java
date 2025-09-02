package com.example.thirdtool.Card.application.service;


import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardRank;
import com.example.thirdtool.Card.domain.model.CardRankType;
import com.example.thirdtool.Card.domain.repository.CardRankRepository;
import com.example.thirdtool.Card.domain.repository.CardRepository;
import com.example.thirdtool.Card.presentation.dto.CardInfoDto;
import com.example.thirdtool.Card.presentation.dto.FeedbackRequestDto;
import com.example.thirdtool.Card.presentation.dto.WriteCardDto;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import com.example.thirdtool.Scoring.aapplication.service.CardAlgorithmService;
import com.example.thirdtool.Scoring.domain.model.ScoringAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
@Service
public class CardService {

    private final CardRepository cardRepository;
    private final DeckRepository deckRepository;
    private final CardRankRepository cardRankRepository;

    private final CardAlgorithmService cardAlgorithmService; // ✅

    private static final int PERMANENT_THRESHOLD_SCORE = 300;

    //카드 만들기
    @Transactional
    public void createCard(Long deckId, WriteCardDto writeCardDto) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new IllegalArgumentException("Deck not found"));

        Card card = Card.of(
                writeCardDto.question(),
                writeCardDto.answer(),
                deck);

        cardRepository.save(card);
    }

    // ✅ 카드 수정하기
    @Transactional
    public void updateCard(Long cardId, WriteCardDto writeCardDto) {
        Card card = cardRepository.findById(cardId)
                                  .orElseThrow(() -> new IllegalArgumentException("Card not found"));
        card.updateCard(writeCardDto.question(), writeCardDto.answer());
    }

    // ✅ 카드 삭제하기
    @Transactional
    public void deleteCard(Long cardId) {
        cardRepository.deleteById(cardId);
    }

    // ✅ 덱 ID를 기반으로 모든 카드 목록 조회 및 덱 accessed 시간 업데이트
    @Transactional
    public List<Card> getCardsByDeckId(Long deckId) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new IllegalArgumentException("Deck not found"));
        deck.updateLastAccessed();
        return cardRepository.findByDeckId(deckId);
    }

    //학습 피드백 및 점수 조절: giveFeedback 메서드는 FeedbackRequestDto를 받아 카드의
    // score, repetition,
    // easinessFactor 등을 SM-2 알고리즘 기반으로 업데이트합니다.
    //
    //"again, hard, normal, good 기반 스코어 조절 시스템" 요구사항을 충족합니다.
    @Transactional
    public void giveFeedback(FeedbackRequestDto feedbackDto) {
        Card card = cardRepository.findById(feedbackDto.cardId())
                                  .orElseThrow(() -> new IllegalArgumentException("카드가 날라갔으요"));

        // ✅ 카드가 속한 덱에서 알고리즘 타입을 가져옵니다.
        String algorithmType = card.getDeck().getScoringAlgorithmType();
        ScoringAlgorithm algorithm = cardAlgorithmService.getAlgorithm(algorithmType);


        card.updateScoreWithAlgorithm(algorithm, feedbackDto.feedback());

        // 점수 임계값을 넘으면 모드를 PERMANENT로 변경
        if (card.getScore() >= PERMANENT_THRESHOLD_SCORE) {
            card.updateMode(DeckMode.PERMANENT);
        }
    }

    // 모드 전환: giveFeedback 메서드 내에 score가 300 이상일 때
    // Permanent 모드로 전환하는 로직이 구현되어 있습니다.
    @Transactional(readOnly = true)
    public List<Card> getCardsByDeckIdAndMode(Long deckId, DeckMode mode) {
        // Deck ID와 모드를 기준으로 카드 목록을 조회합니다.
        return cardRepository.findByDeckIdAndMode(deckId, mode);
    }

    //랭크 기반 카드 조회: getCardsByRank 메서드는 사용자 ID와 rankName을 받아,
    // 해당 사용자의 점수 기준에 맞는 카드들을 필터링하여 반환합니다.
    //"실버 버튼, 골드버튼, 다이아 버튼 구독자 잡기 시스템 (score 기반)" 요구사항을 충족합니다.
    // ✅ 랭크 기반 카드 조회 메서드에 mode 매개변수 추가
    // ✅ 랭크 이름으로 카드 목록을 가져오는 메서드
    // 최종 쿼리 dsl 안으로 모드 확인하는 것은 넣었습니다.
    @Transactional(readOnly = true)
    public List<CardInfoDto> getCardsByRank(Long userId, Long deckId,CardRankType rankName) { // ✅ deckId 추가
        List<CardInfoDto> cards = cardRepository.findCardsByRankWithQuerydsl(userId, deckId, rankName, DeckMode.THREE_DAY);

        if (cards.isEmpty()) {
            throw new IllegalArgumentException("해당 랭크에 해당하는 카드를 찾을 수 없습니다.");
        }

        return cards;
    }

    public Card getCardById(Long cardId) {
        return cardRepository.findById(cardId)
                             .orElseThrow(() -> new IllegalArgumentException("카드가 없습니다!"));
    }

    // ✅ 카드 초기화 및 점수 설정 메서드 수정
    @Transactional
    public void resetCardWithScore(Long cardId, int newScore) {
        Card card = cardRepository.findById(cardId)
                                  .orElseThrow(() -> new IllegalArgumentException("카드가 없는것 같습니다!"));

        // ✅ 모드가 PERMANENT일 때만 초기화 가능하도록 조건 추가
        if (card.getMode() != DeckMode.PERMANENT) {
            throw new IllegalArgumentException("영구 모드에 있는 카드만 초기화할 수 있습니다.");
        }

        card.resetLearningMetrics(newScore);
    }

    @Transactional(readOnly = true)
    public List<Card> getTopNLowScoreCardsForLearningSession(Long deckId, DeckMode mode, int count) {
        Deck deck = deckRepository.findById(deckId)
                                  .orElseThrow(() -> new IllegalArgumentException("Deck not found"));
        deck.updateLastAccessed();

        // 1. 점수가 낮은 순서대로 상위 N개(count)의 카드를 조회
        PageRequest pageable = PageRequest.of(0, count);
        List<Card> lowScoreCards = cardRepository.findByDeckIdAndModeOrderByScoreAsc(deckId, mode, pageable);

        // 2. 선별된 카드들의 순서를 무작위로 섞습니다.
        Collections.shuffle(lowScoreCards);

        return lowScoreCards;
    }

    // ✅ 랭크와 모드 기준으로 점수가 낮은 N개의 카드를 무작위로 가져오는 메서드
    @Transactional(readOnly = true)
    public List<Card> getTopNCardsByRankAndModeForLearning(Long userId, String rankName, DeckMode mode, int count) {
        Deck deck = deckRepository.findById(userId) // TODO: userId 기반 덱 조회 로직으로 변경 필요
                                  .orElseThrow(() -> new IllegalArgumentException("User's deck not found"));
        deck.updateLastAccessed();

        // 1. 랭크와 모드 조건에 맞는 카드를 점수 순으로 N개 조회
        List<Card> selectedCards = cardRepository.findTopNCardsByRankAndMode(userId, rankName, mode, count);

        // 2. 학습 세션의 무작위성을 위해 순서를 섞습니다.
        Collections.shuffle(selectedCards);

        return selectedCards;
    }


}