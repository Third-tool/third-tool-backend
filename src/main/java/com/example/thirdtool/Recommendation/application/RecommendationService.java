package com.example.thirdtool.Recommendation.application;

import com.example.thirdtool.Common.Exception.BusinessException;
import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.Deck.domain.repository.DeckQueryRepository;
import com.example.thirdtool.Deck.domain.repository.DeckRepository;
import com.example.thirdtool.Recommendation.domain.DeckAggResult;
import com.example.thirdtool.Recommendation.domain.DeckRecommendation;
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

    public List<DeckRecommendation> recommendDecks(UserEntity user, int limit) {
        List<DeckAggResult> results = deckQueryRepository.aggregateDeckStats(user);

        return results.stream()
                      .map(DeckAggResult::toDomain)
                      .map(DeckRecommendation::calculateRecommendation)
                      .sorted(Comparator.comparingDouble(DeckRecommendation::getPriorityScore).reversed())
                      .limit(limit)
                      .toList();
    }

    public DeckRecommendation explainRecommendation(Long deckId, UserEntity user) {
        return deckQueryRepository.aggregateDeckStats(user).stream()
                                  .filter(r -> r.deckId().equals(deckId))
                                  .findFirst()
                                  .map(DeckAggResult::toDomain)
                                  .map(DeckRecommendation::calculateRecommendation)
                                  .orElseThrow(() -> new BusinessException(ErrorCode.DECK_NOT_FOUND));
    }
}