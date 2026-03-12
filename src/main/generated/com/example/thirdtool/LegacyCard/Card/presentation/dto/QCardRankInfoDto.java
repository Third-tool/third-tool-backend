package com.example.thirdtool.LegacyCard.Card.presentation.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.example.thirdtool.LegacyCard.Card.presentation.dto.QCardRankInfoDto is a Querydsl Projection type for CardRankInfoDto
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QCardRankInfoDto extends ConstructorExpression<CardRankInfoDto> {

    private static final long serialVersionUID = 2008259803L;

    public QCardRankInfoDto(com.querydsl.core.types.Expression<Long> id, com.querydsl.core.types.Expression<String> question, com.querydsl.core.types.Expression<String> answer, com.querydsl.core.types.Expression<String> thumbnailUrl) {
        super(CardRankInfoDto.class, new Class<?>[]{long.class, String.class, String.class, String.class}, id, question, answer, thumbnailUrl);
    }

}

