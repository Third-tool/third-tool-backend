package com.example.thirdtool.Library.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLibraryEntry is a Querydsl query type for LibraryEntry
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLibraryEntry extends EntityPathBase<LibraryEntry> {

    private static final long serialVersionUID = 510100110L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLibraryEntry libraryEntry = new QLibraryEntry("libraryEntry");

    public final com.example.thirdtool.Common.QBaseEntity _super = new com.example.thirdtool.Common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final com.example.thirdtool.Deck.domain.model.QDeck deck;

    public final StringPath deckNameSnapshot = createString("deckNameSnapshot");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.example.thirdtool.User.domain.model.QUserEntity owner;

    public final BooleanPath publicVisible = createBoolean("publicVisible");

    public final DateTimePath<java.time.LocalDateTime> publishedAt = createDateTime("publishedAt", java.time.LocalDateTime.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public QLibraryEntry(String variable) {
        this(LibraryEntry.class, forVariable(variable), INITS);
    }

    public QLibraryEntry(Path<? extends LibraryEntry> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLibraryEntry(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLibraryEntry(PathMetadata metadata, PathInits inits) {
        this(LibraryEntry.class, metadata, inits);
    }

    public QLibraryEntry(Class<? extends LibraryEntry> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.deck = inits.isInitialized("deck") ? new com.example.thirdtool.Deck.domain.model.QDeck(forProperty("deck"), inits.get("deck")) : null;
        this.owner = inits.isInitialized("owner") ? new com.example.thirdtool.User.domain.model.QUserEntity(forProperty("owner")) : null;
    }

}

