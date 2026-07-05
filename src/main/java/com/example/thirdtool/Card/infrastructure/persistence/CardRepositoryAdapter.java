package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.infrastructure.dto.CardSearchCondition;
import com.example.thirdtool.Card.infrastructure.dto.CardSummaryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CardRepositoryAdapter implements CardRepository {

    private final CardJpaRepository cardJpaRepository;

    @Override
    public Card save(Card card) {
        return cardJpaRepository.save(card);
    }

    /**
     * ID로 카드 단건 조회.
     * keywordCues를 페치 조인으로 함께 로딩한다 (N+1 방지).
     * 논리 삭제 여부 필터링은 서비스 레이어(CardQueryService)에서 처리한다.
     */
    @Override
    public Optional<Card> findById(Long id) {
        return cardJpaRepository.findByIdWithKeywords(id);
    }

    /**
     * @deprecated LT-E5-S5-2 (M5) · 폐기 대기.
     */
    @Deprecated
    @Override
    public List<Card> findAllByDeckIdAndDeletedFalse(Long deckId) {
        return cardJpaRepository.findAllByDeckIdAndDeletedFalse(deckId);
    }

    /**
     * LT-E5-S5-2 (M5) — 축 스코프 카드 조회. Spring Data 네이밍 규칙 자동 도출.
     */
    @Override
    public List<Card> findAllByAxisIdAndDeletedFalse(Long axisId) {
        return cardJpaRepository.findAllByAxisIdAndDeletedFalse(axisId);
    }

    /**
     * 카드 검색 (QueryDSL Projection + 동적 조건 + 페이징).
     * 논리 삭제된 카드 제외는 {@link CardJpaRepositoryImpl} 내부에서 처리된다.
     */
    @Override
    public Page<CardSummaryRow> searchCards(CardSearchCondition condition, Pageable pageable) {
        return cardJpaRepository.searchCards(condition, pageable);
    }

    @Override
    public List<Card> findBySharedTagIds(List<Long> tagIds, Long excludeCardId) {
        if (tagIds == null || tagIds.isEmpty()) return List.of();
        return cardJpaRepository.findBySharedTagIds(tagIds, excludeCardId);
    }

    @Override
    public List<Card> findAllByStatus(CardStatus status) {
        return cardJpaRepository.findAllByStatusAndDeletedFalse(status);
    }

    @Override
    public List<Card> findByTagIdAndUserIdAndDeletedFalse(Long tagId, Long userId) {
        return cardJpaRepository.findByTagIdAndUserIdAndDeletedFalse(tagId, userId);
    }

    @Override
    public List<Card> findArchivedBySharedTagIdsAndUserId(List<Long> tagIds, Long excludeCardId, Long userId) {
        if (tagIds == null || tagIds.isEmpty()) return List.of();
        return cardJpaRepository.findArchivedBySharedTagIdsAndUserId(tagIds, excludeCardId, userId);
    }

    @Override
    public int detachTagFromUserCards(Long userId, Long tagId) {
        return cardJpaRepository.detachTagFromUserCards(userId, tagId);
    }

    @Override
    public List<Card> findOnFieldEligibleByUserId(Long userId, LocalDateTime threshold) {
        return cardJpaRepository.findOnFieldEligibleByUserId(userId, threshold);
    }

    @Override
    public List<Card> findByUserIdAndAxisIdsAndStatus(
            Long userId, List<Long> axisIds, CardStatus status) {
        if (axisIds == null || axisIds.isEmpty()) return List.of();
        return cardJpaRepository.findByUserIdAndAxisIdsAndStatus(userId, axisIds, status);
    }

    @Override
    public long countByAxisIdAndDeletedFalse(Long axisId) {
        return cardJpaRepository.countByAxisIdAndDeletedFalse(axisId);
    }

    @Override
    public long countByAxisIdAndStatusAndDeletedFalse(Long axisId, CardStatus status) {
        return cardJpaRepository.countByAxisIdAndStatusAndDeletedFalse(axisId, status);
    }
}