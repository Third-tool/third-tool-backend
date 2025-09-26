package com.example.thirdtool.Deck.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDeck is a Querydsl query type for Deck
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDeck extends EntityPathBase<Deck> {

    private static final long serialVersionUID = -942093549L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDeck deck = new QDeck("deck");

    public final ListPath<com.example.thirdtool.Card.domain.model.Card, com.example.thirdtool.Card.domain.model.QCard> cards = this.<com.example.thirdtool.Card.domain.model.Card, com.example.thirdtool.Card.domain.model.QCard>createList("cards", com.example.thirdtool.Card.domain.model.Card.class, com.example.thirdtool.Card.domain.model.QCard.class, PathInits.DIRECT2);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isShared = createBoolean("isShared");

    public final DateTimePath<java.time.LocalDateTime> lastAccessed = createDateTime("lastAccessed", java.time.LocalDateTime.class);

    public final StringPath name = createString("name");

    public final QDeck originalDeck;

    public final QDeck parentDeck;

    public final StringPath scoringAlgorithmType = createString("scoringAlgorithmType");

    public final ListPath<Deck, QDeck> subDecks = this.<Deck, QDeck>createList("subDecks", Deck.class, QDeck.class, PathInits.DIRECT2);

    public final ListPath<com.example.thirdtool.Tag.domain.model.Tag, com.example.thirdtool.Tag.domain.model.QTag> tags = this.<com.example.thirdtool.Tag.domain.model.Tag, com.example.thirdtool.Tag.domain.model.QTag>createList("tags", com.example.thirdtool.Tag.domain.model.Tag.class, com.example.thirdtool.Tag.domain.model.QTag.class, PathInits.DIRECT2);

    public final com.example.thirdtool.User.domain.model.QUser user;

    public QDeck(String variable) {
        this(Deck.class, forVariable(variable), INITS);
    }

    public QDeck(Path<? extends Deck> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDeck(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDeck(PathMetadata metadata, PathInits inits) {
        this(Deck.class, metadata, inits);
    }

    public QDeck(Class<? extends Deck> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.originalDeck = inits.isInitialized("originalDeck") ? new QDeck(forProperty("originalDeck"), inits.get("originalDeck")) : null;
        this.parentDeck = inits.isInitialized("parentDeck") ? new QDeck(forProperty("parentDeck"), inits.get("parentDeck")) : null;
        this.user = inits.isInitialized("user") ? new com.example.thirdtool.User.domain.model.QUser(forProperty("user")) : null;
    }

}

