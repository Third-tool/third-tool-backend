package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.infrastructure.dto.CardSearchCondition;
import com.example.thirdtool.Card.infrastructure.dto.CardSummaryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * Card Aggregate의 영속성 포트.
 *
 * 실제 구현은 infrastructure 레이어의 {@code CardRepositoryImpl}에서 담당한다.
 */
public interface CardRepository {

    Card save(Card card);

    Optional<Card> findById(Long id);

    /**
     * 덱 내 활성 카드 목록 조회 (논리 삭제 제외).
     * CardQueryService.findAllByDeckId()에서 사용.
     */
    List<Card> findAllByDeckIdAndDeletedFalse(Long deckId);

    /**
     * 카드 검색 (QueryDSL Projection, 페이징).
     * 논리 삭제된 카드는 제외한다.
     */
    Page<CardSummaryRow> searchCards(CardSearchCondition condition, Pageable pageable);

}

