package com.example.thirdtool.Card.presentation.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.example.thirdtool.Card.presentation.dto.QCardRankInfoDto is a Querydsl Projection type for CardRankInfoDto
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QCardRankInfoDto extends ConstructorExpression<CardRankInfoDto> {

    private static final long serialVersionUID = 11117104L;

    public QCardRankInfoDto(com.querydsl.core.types.Expression<Long> id, com.querydsl.core.types.Expression<String> question, com.querydsl.core.types.Expression<String> answer, com.querydsl.core.types.Expression<String> thumbnailUrl) {
        super(CardRankInfoDto.class, new Class<?>[]{long.class, String.class, String.class, String.class}, id, question, answer, thumbnailUrl);
    }

}

