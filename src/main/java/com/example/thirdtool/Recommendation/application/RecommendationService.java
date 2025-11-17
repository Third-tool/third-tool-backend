package com.example.thirdtool.Recommendation.application;

import com.example.thirdtool.Card.domain.model.CardImage;
import com.example.thirdtool.Card.domain.repository.CardImageRepository;
import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.repository.DeckQueryRepository;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import com.example.thirdtool.Recommendation.domain.DeckAggResult;
import com.example.thirdtool.Recommendation.domain.DeckRecommendation;
import com.example.thirdtool.Recommendation.domain.dto.RecommendationExplainResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RecommendationService {

    private final DeckQueryRepository deckQueryRepository;
    private final CardImageRepository cardImageRepository;

    public List<DeckRecommendation> recommendDecks(UserEntity user, int limit) {
        List<DeckAggResult> results = deckQueryRepository.aggregateDeckStats(user);

        List<DeckRecommendation> recs = results.stream()
                                               .map(DeckAggResult::toDomain)
                                               .map(DeckRecommendation::calculateRecommendation)
                                               .sorted(Comparator.comparingDouble(DeckRecommendation::getPriorityScore).reversed())
                                               .limit(limit)
                                               .toList();

        // 썸네일 주입 (limit가 작으니 간단 N회 조회 허용)
        recs.forEach(rec -> {
            String url = cardImageRepository.pickDeckThumbnailOne(rec.getDeckId())
                                            .map(CardImage::getImageUrl)
                                            .orElse(null); // FE 폴백(이모티콘/기본 이미지)로 처리
            rec.setThumbnailUrl(url);
        });
        return recs;
    }

    @Transactional(readOnly = true)
    public String explainRecommendation(Long deckId, UserEntity user) {
        return deckQueryRepository.aggregateDeckStats(user).stream()
                                  .filter(x -> x.deckId().equals(deckId))
                                  .findFirst()
                                  .map(DeckAggResult::toDomain)
                                  .map(DeckRecommendation::calculateRecommendation)
                                  .map(DeckRecommendation::getReason) // ✅ 문자열(reason)만 추출
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));
    }

}