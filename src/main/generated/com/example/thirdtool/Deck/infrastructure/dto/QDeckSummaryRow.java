package com.example.thirdtool.Deck.infrastructure.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.example.thirdtool.Deck.infrastructure.dto.QDeckSummaryRow is a Querydsl Projection type for DeckSummaryRow
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QDeckSummaryRow extends ConstructorExpression<DeckSummaryRow> {

    private static final long serialVersionUID = -1034238638L;

    public QDeckSummaryRow(com.querydsl.core.types.Expression<Long> deckId, com.querydsl.core.types.Expression<String> name, com.querydsl.core.types.Expression<com.example.thirdtool.Deck.domain.model.DeckMode> mode, com.querydsl.core.types.Expression<Integer> depth, com.querydsl.core.types.Expression<java.time.LocalDateTime> lastAccessed, com.querydsl.core.types.Expression<Integer> cardCount, com.querydsl.core.types.Expression<Integer> subDeckCount) {
        super(DeckSummaryRow.class, new Class<?>[]{long.class, String.class, com.example.thirdtool.Deck.domain.model.DeckMode.class, int.class, java.time.LocalDateTime.class, int.class, int.class}, deckId, name, mode, depth, lastAccessed, cardCount, subDeckCount);
    }

}

