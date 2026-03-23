package com.example.thirdtool.Review.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCardReview is a Querydsl query type for CardReview
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCardReview extends EntityPathBase<CardReview> {

    private static final long serialVersionUID = 1127348001L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCardReview cardReview = new QCardReview("cardReview");

    public final com.example.thirdtool.Card.domain.model.QCard card;

    public final NumberPath<Integer> cardOrder = createNumber("cardOrder", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> revealedAt = createDateTime("revealedAt", java.time.LocalDateTime.class);

    public final QReviewSession reviewSession;

    public final EnumPath<ReviewStep> reviewStep = createEnum("reviewStep", ReviewStep.class);

    public QCardReview(String variable) {
        this(CardReview.class, forVariable(variable), INITS);
    }

    public QCardReview(Path<? extends CardReview> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCardReview(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCardReview(PathMetadata metadata, PathInits inits) {
        this(CardReview.class, metadata, inits);
    }

    public QCardReview(Class<? extends CardReview> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.card = inits.isInitialized("card") ? new com.example.thirdtool.Card.domain.model.QCard(forProperty("card"), inits.get("card")) : null;
        this.reviewSession = inits.isInitialized("reviewSession") ? new QReviewSession(forProperty("reviewSession"), inits.get("reviewSession")) : null;
    }

}

