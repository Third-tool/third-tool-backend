package com.example.thirdtool.Card.domain.repository;

import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardRank;
import com.example.thirdtool.Card.domain.model.CardRankType;
import com.example.thirdtool.Card.domain.model.QCardRank;
import com.example.thirdtool.Card.presentation.dto.CardInfoDto;
import com.example.thirdtool.Card.presentation.dto.QCardInfoDto;
import com.example.thirdtool.Deck.domain.model.DeckMode;

import com.example.thirdtool.Scoring.domain.model.QLearningProfile;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;

import static com.example.thirdtool.Card.domain.model.QCard.card;
import static com.example.thirdtool.Card.domain.model.QCardRank.cardRank;
import static com.example.thirdtool.Deck.domain.model.QDeck.deck;
import static com.example.thirdtool.Scoring.domain.model.QLearningProfile.learningProfile;


@Repository
@RequiredArgsConstructor
public class CardRepositoryCustomImpl implements CardRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<CardInfoDto> findCardsByScoreRange(Long userId,
                                                    Long deckId,
                                                    DeckMode mode,
                                                    int minScore,
                                                    int maxScore,
                                                    Pageable pageable) {
        QLearningProfile learningProfile = QLearningProfile.learningProfile;

        List<CardInfoDto> results = queryFactory
                .select(new QCardInfoDto(card.id, card.question, card.answer))
                .from(card)
                .join(card.deck, deck)
                .join(card.learningProfile, learningProfile)
                .where(
                        deck.id.eq(deckId),
                        deck.user.id.eq(userId),
                        learningProfile.mode.eq(mode),
                        learningProfile.score.between(minScore, maxScore) // ✅ 점수 조건
                      )
                .orderBy(card.id.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        boolean hasNext = results.size() > pageable.getPageSize();
        if (hasNext) results.remove(pageable.getPageSize());

        return new SliceImpl<>(results, pageable, hasNext);
    }

    @Override
    public List<Card> findTopNCardsByRankAndMode(Long userId, String rankName, DeckMode mode, int count) {

        // 1️⃣ CardRank 범위 가져오기
        CardRank rank = queryFactory
                .selectFrom(cardRank)
                .where(cardRank.user.id.eq(userId)
                                       .and(cardRank.name.eq(rankName)))
                .fetchOne();

        if (rank == null) return Collections.emptyList();

        // 2️⃣ 점수 범위 내에서 상위 N개 카드 조회
        return queryFactory.selectFrom(card)
                           .join(card.learningProfile, learningProfile).fetchJoin()
                           .where(
                                   card.learningProfile.mode.eq(mode),
                                   card.learningProfile.score.between(rank.getMinScore(), rank.getMaxScore())
                                 )
                           .orderBy(card.learningProfile.score.asc())
                           .limit(count)
                           .fetch();
    }

    @Override
    public int countByRankAndMode(Long userId, String rankName, DeckMode mode) {
        CardRank rank = queryFactory
                .selectFrom(cardRank)
                .where(cardRank.user.id.eq(userId)
                                       .and(cardRank.name.eq(rankName)))
                .fetchOne();

        if (rank == null) return 0;

        Long count = queryFactory.select(card.count())
                                 .from(card)
                                 .join(card.learningProfile, learningProfile)
                                 .where(
                                         card.learningProfile.mode.eq(mode),
                                         card.learningProfile.score.between(rank.getMinScore(), rank.getMaxScore())
                                       )
                                 .fetchOne();

        return count != null ? count.intValue() : 0;
    }
}