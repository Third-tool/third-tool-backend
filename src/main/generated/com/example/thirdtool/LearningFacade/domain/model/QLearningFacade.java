package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLearningFacade is a Querydsl query type for LearningFacade
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLearningFacade extends EntityPathBase<LearningFacade> {

    private static final long serialVersionUID = 589724529L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLearningFacade learningFacade = new QLearningFacade("learningFacade");

    public final ListPath<LearningAxis, QLearningAxis> axes = this.<LearningAxis, QLearningAxis>createList("axes", LearningAxis.class, QLearningAxis.class, PathInits.DIRECT2);

    public final StringPath concept = createString("concept");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final com.example.thirdtool.User.domain.model.QUserEntity user;

    public QLearningFacade(String variable) {
        this(LearningFacade.class, forVariable(variable), INITS);
    }

    public QLearningFacade(Path<? extends LearningFacade> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLearningFacade(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLearningFacade(PathMetadata metadata, PathInits inits) {
        this(LearningFacade.class, metadata, inits);
    }

    public QLearningFacade(Class<? extends LearningFacade> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.example.thirdtool.User.domain.model.QUserEntity(forProperty("user")) : null;
    }

}

