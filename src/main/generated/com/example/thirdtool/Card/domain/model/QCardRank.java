package com.example.thirdtool.Card.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCardRank is a Querydsl query type for CardRank
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCardRank extends EntityPathBase<CardRank> {

    private static final long serialVersionUID = -779394067L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCardRank cardRank = new QCardRank("cardRank");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> maxScore = createNumber("maxScore", Integer.class);

    public final NumberPath<Integer> minScore = createNumber("minScore", Integer.class);

    public final StringPath name = createString("name");

    public final com.example.thirdtool.User.domain.model.QUserEntity user;

    public QCardRank(String variable) {
        this(CardRank.class, forVariable(variable), INITS);
    }

    public QCardRank(Path<? extends CardRank> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCardRank(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCardRank(PathMetadata metadata, PathInits inits) {
        this(CardRank.class, metadata, inits);
    }

    public QCardRank(Class<? extends CardRank> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.example.thirdtool.User.domain.model.QUserEntity(forProperty("user")) : null;
    }

}

