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

    public final NumberPath<Integer> badCount = createNumber("badCount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final com.example.thirdtool.Deck.domain.model.QDeck deck;

    public final NumberPath<Double> easinessFactor = createNumber("easinessFactor", Double.class);

    public final NumberPath<Integer> goodCount = createNumber("goodCount", Integer.class);

    public final NumberPath<Integer> greatCount = createNumber("greatCount", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<com.example.thirdtool.Deck.domain.model.DeckMode> mode = createEnum("mode", com.example.thirdtool.Deck.domain.model.DeckMode.class);

    public final NumberPath<Integer> normalCount = createNumber("normalCount", Integer.class);

    public final StringPath question = createString("question");

    public final NumberPath<Integer> repetition = createNumber("repetition", Integer.class);

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

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
    }

}

