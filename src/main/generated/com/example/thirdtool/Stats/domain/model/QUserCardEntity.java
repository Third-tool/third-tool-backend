package com.example.thirdtool.Stats.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserCardEntity is a Querydsl query type for UserCardEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserCardEntity extends EntityPathBase<UserCardEntity> {

    private static final long serialVersionUID = -1198293090L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserCardEntity userCardEntity = new QUserCardEntity("userCardEntity");

    public final com.example.thirdtool.Card.domain.model.QCard card;

    public final NumberPath<Integer> correctCount = createNumber("correctCount", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> incorrectCount = createNumber("incorrectCount", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> lastStudy = createDateTime("lastStudy", java.time.LocalDateTime.class);

    public final NumberPath<Integer> studyCount = createNumber("studyCount", Integer.class);

    public final com.example.thirdtool.User.domain.model.QUser user;

    public QUserCardEntity(String variable) {
        this(UserCardEntity.class, forVariable(variable), INITS);
    }

    public QUserCardEntity(Path<? extends UserCardEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserCardEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserCardEntity(PathMetadata metadata, PathInits inits) {
        this(UserCardEntity.class, metadata, inits);
    }

    public QUserCardEntity(Class<? extends UserCardEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.card = inits.isInitialized("card") ? new com.example.thirdtool.Card.domain.model.QCard(forProperty("card"), inits.get("card")) : null;
        this.user = inits.isInitialized("user") ? new com.example.thirdtool.User.domain.model.QUser(forProperty("user")) : null;
    }

}

