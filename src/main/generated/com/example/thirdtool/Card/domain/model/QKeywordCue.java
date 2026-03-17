package com.example.thirdtool.Card.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QKeywordCue is a Querydsl query type for KeywordCue
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QKeywordCue extends EntityPathBase<KeywordCue> {

    private static final long serialVersionUID = -2042296357L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QKeywordCue keywordCue = new QKeywordCue("keywordCue");

    public final QCard card;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath value = createString("value");

    public QKeywordCue(String variable) {
        this(KeywordCue.class, forVariable(variable), INITS);
    }

    public QKeywordCue(Path<? extends KeywordCue> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QKeywordCue(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QKeywordCue(PathMetadata metadata, PathInits inits) {
        this(KeywordCue.class, metadata, inits);
    }

    public QKeywordCue(Class<? extends KeywordCue> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.card = inits.isInitialized("card") ? new QCard(forProperty("card"), inits.get("card")) : null;
    }

}

