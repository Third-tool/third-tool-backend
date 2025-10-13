package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Scoring.domain.model.LearningProfile;
import com.example.thirdtool.Card.domain.repository.CardRepository;
import com.example.thirdtool.Card.presentation.dto.request.FeedbackRequestDto;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.Scoring.aapplication.service.CardAlgorithmService;
import com.example.thirdtool.Scoring.domain.model.algorithm.ScoringAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CardFeedbackService {

    private final CardRepository cardRepository;
    private final CardAlgorithmService cardAlgorithmService;

    private static final int PERMANENT_THRESHOLD_SCORE = 300;

    /**
     * 학습 피드백 및 점수 조절
     * - FeedbackRequestDto를 받아 카드의 학습 프로필을 업데이트
     * - "again, hard, normal, good" 기반 점수 조절
     */
    public void giveFeedback(FeedbackRequestDto dto) {
        //있는 카드인지 확인
        Card card = cardRepository.findById(dto.cardId())
                                  .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));


        LearningProfile profile = card.getLearningProfile();
        try {
            profile.ensureFeedbackAllowed();
        } catch (IllegalStateException e) { // 기존 로직이 예외를 던진다면 래핑 (혹은 ensureFeedbackAllowed 자체에서 BusinessException 던지도록 수정)
            throw new BusinessException(ErrorCode.CARD_FEEDBACK_NOT_ALLOWED);
        }

        String algorithmType = card.getScoringAlgorithmType();
        ScoringAlgorithm algorithm = cardAlgorithmService.getAlgorithm(algorithmType);

        // ✅ 학습 프로필이 직접 피드백 처리
        profile.applyFeedback(algorithm, card, dto.feedback());

        // ✅ 점수 기반 DeckMode 전환도 학습 프로필이 관리
        if (profile.getScore() >= PERMANENT_THRESHOLD_SCORE) {
            profile.updateMode(DeckMode.PERMANENT);
        }

    }

    /**
     * 카드 초기화 및 점수 설정
     * - PERMANENT 모드일 때만 초기화 가능
     */
    @Transactional
    public void resetCardWithScore(Long cardId, int newScore) {
        Card card = cardRepository.findById(cardId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        LearningProfile profile = card.getLearningProfile();
        if (profile.getMode() != DeckMode.PERMANENT) {
            throw new BusinessException(ErrorCode.CARD_RESET_NOT_ALLOWED);
        }
        profile.reset(newScore);
        profile.updateMode(DeckMode.THREE_DAY);
    }
}