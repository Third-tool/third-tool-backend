package com.example.thirdtool.Review.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QReviewSession is a Querydsl query type for ReviewSession
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QReviewSession extends EntityPathBase<ReviewSession> {

    private static final long serialVersionUID = -765683483L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QReviewSession reviewSession = new QReviewSession("reviewSession");

    public final NumberPath<Integer> availableCardCount = createNumber("availableCardCount", Integer.class);

    public final ListPath<CardReview, QCardReview> cardReviews = this.<CardReview, QCardReview>createList("cardReviews", CardReview.class, QCardReview.class, PathInits.DIRECT2);

    public final NumberPath<Integer> currentIndex = createNumber("currentIndex", Integer.class);

    public final com.example.thirdtool.Deck.domain.model.QDeck deck;

    public final BooleanPath finished = createBoolean("finished");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> startedAt = createDateTime("startedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> totalCardCount = createNumber("totalCardCount", Integer.class);

    public final com.example.thirdtool.User.domain.model.QUserEntity user;

    public QReviewSession(String variable) {
        this(ReviewSession.class, forVariable(variable), INITS);
    }

    public QReviewSession(Path<? extends ReviewSession> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QReviewSession(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QReviewSession(PathMetadata metadata, PathInits inits) {
        this(ReviewSession.class, metadata, inits);
    }

    public QReviewSession(Class<? extends ReviewSession> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.deck = inits.isInitialized("deck") ? new com.example.thirdtool.Deck.domain.model.QDeck(forProperty("deck"), inits.get("deck")) : null;
        this.user = inits.isInitialized("user") ? new com.example.thirdtool.User.domain.model.QUserEntity(forProperty("user")) : null;
    }

}

