package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.infrastructure.dto.CardSearchCondition;
import com.example.thirdtool.Card.infrastructure.dto.CardSummaryRow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

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
     * 덱 내 활성 카드 목록 조회.
     * deleted = false 조건은 JpaRepository 네이밍 규칙으로 자동 처리된다.
     */
    @Override
    public List<Card> findAllByDeckIdAndDeletedFalse(Long deckId) {
        return cardJpaRepository.findAllByDeckIdAndDeletedFalse(deckId);
    }

    /**
     * 카드 검색 (QueryDSL Projection + 동적 조건 + 페이징).
     * 논리 삭제된 카드 제외는 {@link CardJpaRepositoryImpl} 내부에서 처리된다.
     */
    @Override
    public Page<CardSummaryRow> searchCards(CardSearchCondition condition, Pageable pageable) {
        return cardJpaRepository.searchCards(condition, pageable);
    }
}