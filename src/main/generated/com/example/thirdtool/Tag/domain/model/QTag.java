package com.example.thirdtool.Tag.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTag is a Querydsl query type for Tag
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTag extends EntityPathBase<Tag> {

    private static final long serialVersionUID = -156513377L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTag tag = new QTag("tag");

    public final SetPath<com.example.thirdtool.Deck.domain.model.Deck, com.example.thirdtool.Deck.domain.model.QDeck> decks = this.<com.example.thirdtool.Deck.domain.model.Deck, com.example.thirdtool.Deck.domain.model.QDeck>createSet("decks", com.example.thirdtool.Deck.domain.model.Deck.class, com.example.thirdtool.Deck.domain.model.QDeck.class, PathInits.DIRECT2);

    public final StringPath displayName = createString("displayName");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath nameKey = createString("nameKey");

    public final com.example.thirdtool.User.domain.model.QUser user;

    public QTag(String variable) {
        this(Tag.class, forVariable(variable), INITS);
    }

    public QTag(Path<? extends Tag> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTag(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTag(PathMetadata metadata, PathInits inits) {
        this(Tag.class, metadata, inits);
    }

    public QTag(Class<? extends Tag> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.example.thirdtool.User.domain.model.QUser(forProperty("user")) : null;
    }

}

