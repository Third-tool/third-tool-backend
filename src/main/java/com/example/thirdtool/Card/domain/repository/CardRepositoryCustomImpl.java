package com.example.thirdtool.Card.domain.repository;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardRank;
import com.example.thirdtool.Card.domain.model.CardRankType;
import com.example.thirdtool.Card.domain.model.QCardRank;
import com.example.thirdtool.Card.presentation.dto.CardInfoDto;
import com.example.thirdtool.Deck.domain.model.DeckMode;
import com.example.thirdtool.User.domain.model.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.example.thirdtool.Card.domain.model.QCard.card;
import static com.example.thirdtool.Card.domain.model.QCardRank.cardRank;
import static com.example.thirdtool.User.domain.model.QUser.user;

@RequiredArgsConstructor
@Repository
public class CardRepositoryCustomImpl implements CardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<CardInfoDto> findCardsByRankWithQuerydsl(Long userId, Long deckId, CardRankType rankName, DeckMode mode) {
        // 랭크의 점수 범위를 찾기 위한 서브쿼리
        CardRank rankInfo = queryFactory
                .selectFrom(cardRank)
                .where(cardRank.user.id.eq(userId).and(cardRank.name.eq(rankName.name()))) // ✅ .name() 추가
                .fetchOne();

        if (rankInfo == null) {
            return List.of();
        }

        // 메인 쿼리: 랭크의 점수 범위를 사용해 카드를 조회
        BooleanExpression scoreBetween = card.score.between(rankInfo.getMinScore(), rankInfo.getMaxScore());
        BooleanExpression deckModeEq = card.mode.eq(mode);
        BooleanExpression deckIdEq = card.deck.id.eq(deckId);

        return queryFactory
                .select(Projections.constructor(CardInfoDto.class,
                        card.id, card.question, card.answer, card.score, card.mode)) // ✅ score와 mode 필드 추가
                .from(card)
                .where(scoreBetween.and(deckModeEq).and(deckIdEq))
                .fetch();
    }

    // ✅ 랭크, 모드 기준으로 점수가 낮은 상위 N개의 카드를 조회하는 로직
    @Override
    public List<Card> findTopNCardsByRankAndMode(Long userId, String rankName, DeckMode mode, int count) {
        return queryFactory
                .selectFrom(card)
                .join(cardRank).on(card.score.between(cardRank.minScore, cardRank.maxScore)
                                             .and(cardRank.user.id.eq(userId))
                                             .and(cardRank.name.eq(rankName)))
                .where(card.mode.eq(mode))
                .orderBy(card.score.asc()) // 점수가 낮은 순서로 정렬
                .limit(count) // N개만 가져오기
                .fetch();
    }
}