package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.application.resolver.PermanentThresholdResolver;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.DailyLearningProgress.application.DailyLearningProgressService;
import com.example.thirdtool.Scoring.domain.model.LearningProfile;
import com.example.thirdtool.Card.domain.repository.CardRepository;
import com.example.thirdtool.Card.presentation.dto.request.FeedbackRequestDto;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.Scoring.aapplication.service.CardAlgorithmService;
import com.example.thirdtool.Scoring.domain.model.algorithm.ScoringAlgorithm;
import com.example.thirdtool.Scoring.domain.repository.LearningProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardFeedbackService {

    private final CardRepository cardRepository;
    private final LearningProfileRepository learningProfileRepository;

    private final CardAlgorithmService cardAlgorithmService;
    private final DailyLearningProgressService dailyLearningProgressService;
    private final PermanentThresholdResolver permanentThresholdResolver;

    public void giveFeedback(Long userId, FeedbackRequestDto dto) {
        // 1️⃣ 프로필 로드 (이제 Profile이 FK를 가짐)
        LearningProfile profile = learningProfileRepository.findByCardId(dto.cardId())
                                                           .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        // 2️⃣ Card도 필요할 경우 Profile로부터 접근
        Card card = profile.getCard();

        log.info("[Feedback] 카드 ID={}, 프로필 ID={}, 실제 클래스={}, 알고리즘 타입={}",
                card.getId(),
                profile.getId(),
                profile.getClass().getSimpleName(),
                profile.getAlgorithmType());

        // 3️⃣ 모드 체크
        try {
            profile.ensureFeedbackAllowed();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.CARD_FEEDBACK_NOT_ALLOWED);
        }

        // 4️⃣ 알고리즘 선택
        ScoringAlgorithm algorithm = cardAlgorithmService.getAlgorithm(profile.getAlgorithmType());
        log.debug("[Feedback] 선택된 알고리즘 Bean: {}", algorithm.getClass().getSimpleName());

        // 5️⃣ 피드백 적용 (프로필 중심)
        profile.applyFeedback(algorithm, card, dto.feedback());

        // 6️⃣ 임계값 계산 후 모드 전환
        int threshold = permanentThresholdResolver.resolveForUser(userId);
        if (profile.getScore() >= threshold) {
            log.info("[Feedback] 카드 {} → PERMANENT 모드 전환 (score={}, threshold={})",
                    card.getId(), profile.getScore(), threshold);
            profile.updateMode(DeckMode.PERMANENT);
        }

        // 7️⃣ 랭크 카운트 증가 (optional)
        if (dto.rankType() != null) {
            dailyLearningProgressService.increaseRankCount(userId, dto.rankType());
        }
    }


    @Transactional
    public void resetCardToSilverMin(Long cardId) {
        Card card = cardRepository.findById(cardId)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        LearningProfile profile = card.getLearningProfile();
        if (profile.getMode() != DeckMode.PERMANENT) {
            throw new BusinessException(ErrorCode.CARD_RESET_NOT_ALLOWED);
        }

        Long userId = card.getDeck().getUser().getId();
        int silverMin = permanentThresholdResolver.resolveSilverMinForUser(userId);

        profile.reset(silverMin);
        profile.updateMode(DeckMode.THREE_DAY);
    }

}