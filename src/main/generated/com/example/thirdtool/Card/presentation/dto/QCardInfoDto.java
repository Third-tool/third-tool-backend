package com.example.thirdtool.Card.presentation.dto;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.example.thirdtool.Card.presentation.dto.QCardInfoDto is a Querydsl Projection type for CardInfoDto
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QCardInfoDto extends ConstructorExpression<CardInfoDto> {

    private static final long serialVersionUID = 2123345276L;

    public QCardInfoDto(com.querydsl.core.types.Expression<Long> id, com.querydsl.core.types.Expression<String> question, com.querydsl.core.types.Expression<String> answer) {
        super(CardInfoDto.class, new Class<?>[]{long.class, String.class, String.class}, id, question, answer);
    }

}

