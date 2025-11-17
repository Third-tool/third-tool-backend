package com.example.thirdtool.Card.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCard is a Querydsl query type for Card
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCard extends EntityPathBase<Card> {

    private static final long serialVersionUID = 29465633L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCard card = new QCard("card");

    public final com.example.thirdtool.Common.QBaseEntity _super = new com.example.thirdtool.Common.QBaseEntity(this);

    public final StringPath answer = createString("answer");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final com.example.thirdtool.Deck.domain.model.QDeck deck;

    public final BooleanPath deleted = createBoolean("deleted");

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<CardImage, QCardImage> images = this.<CardImage, QCardImage>createList("images", CardImage.class, QCardImage.class, PathInits.DIRECT2);

    public final com.example.thirdtool.Scoring.domain.model.QLearningProfile learningProfile;

    public final StringPath question = createString("question");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public QCard(String variable) {
        this(Card.class, forVariable(variable), INITS);
    }

    public QCard(Path<? extends Card> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCard(PathMetadata metadata, PathInits inits) {
        this(Card.class, metadata, inits);
    }

    public QCard(Class<? extends Card> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.deck = inits.isInitialized("deck") ? new com.example.thirdtool.Deck.domain.model.QDeck(forProperty("deck"), inits.get("deck")) : null;
        this.learningProfile = inits.isInitialized("learningProfile") ? new com.example.thirdtool.Scoring.domain.model.QLearningProfile(forProperty("learningProfile"), inits.get("learningProfile")) : null;
    }

}

