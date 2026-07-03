package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLearningLayer is a Querydsl query type for LearningLayer
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLearningLayer extends EntityPathBase<LearningLayer> {

    private static final long serialVersionUID = -668150886L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLearningLayer learningLayer = new QLearningLayer("learningLayer");

    public final ListPath<LearningAxis, QLearningAxis> axes = this.<LearningAxis, QLearningAxis>createList("axes", LearningAxis.class, QLearningAxis.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> displayOrder = createNumber("displayOrder", Integer.class);

    public final QLearningFacade facade;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QLearningLayer(String variable) {
        this(LearningLayer.class, forVariable(variable), INITS);
    }

    public QLearningLayer(Path<? extends LearningLayer> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLearningLayer(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLearningLayer(PathMetadata metadata, PathInits inits) {
        this(LearningLayer.class, metadata, inits);
    }

    public QLearningLayer(Class<? extends LearningLayer> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.facade = inits.isInitialized("facade") ? new QLearningFacade(forProperty("facade"), inits.get("facade")) : null;
    }

}

