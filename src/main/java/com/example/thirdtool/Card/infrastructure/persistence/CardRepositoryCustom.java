package com.example.thirdtool.Card.infrastructure.persistence;

import com.example.thirdtool.Card.infrastructure.dto.CardSearchCondition;
import com.example.thirdtool.Card.infrastructure.dto.CardSummaryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * QueryDSL 기반 Card 커스텀 조회 인터페이스.
 */
public interface CardRepositoryCustom {

    Page<CardSummaryRow> searchCards(CardSearchCondition condition, Pageable pageable);
}
