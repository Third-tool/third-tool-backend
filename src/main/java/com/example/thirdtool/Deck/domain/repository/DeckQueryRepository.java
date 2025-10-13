package com.example.thirdtool.Deck.domain.repository;

import com.example.thirdtool.Card.domain.model.QCard;
import com.example.thirdtool.Deck.domain.model.QDeck;
import com.example.thirdtool.Recommendation.domain.DeckAggResult;
import com.example.thirdtool.Scoring.domain.model.QLearningProfile;
import com.example.thirdtool.User.domain.model.UserEntity;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class DeckQueryRepository {

    private final JPAQueryFactory queryFactory;

    public List<DeckAggResult> aggregateDeckStats(UserEntity user) {
        QDeck deck = QDeck.deck;
        QCard card = QCard.card;
        QLearningProfile lp = QLearningProfile.learningProfile;

        return queryFactory
                .select(Projections.constructor(
                        DeckAggResult.class,
                        deck.id,
                        deck.name,
                        lp.score.avg(),
                        lp.badCount.sum(),
                        lp.badCount.add(lp.normalCount).add(lp.goodCount).add(lp.greatCount).sum(),
                        deck.lastAccessed.max(),
                        card.id.count()
                                               ))
                .from(deck)
                .join(deck.cards, card)
                .join(card.learningProfile, lp)
                .where(deck.user.eq(user))
                .groupBy(deck.id, deck.name)
                .fetch();
    }
}
