package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLearningFacadeConcept is a Querydsl query type for LearningFacadeConcept
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLearningFacadeConcept extends EntityPathBase<LearningFacadeConcept> {

    private static final long serialVersionUID = -856291913L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLearningFacadeConcept learningFacadeConcept = new QLearningFacadeConcept("learningFacadeConcept");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> displayOrder = createNumber("displayOrder", Integer.class);

    public final QLearningFacade facade;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath value = createString("value");

    public QLearningFacadeConcept(String variable) {
        this(LearningFacadeConcept.class, forVariable(variable), INITS);
    }

    public QLearningFacadeConcept(Path<? extends LearningFacadeConcept> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLearningFacadeConcept(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLearningFacadeConcept(PathMetadata metadata, PathInits inits) {
        this(LearningFacadeConcept.class, metadata, inits);
    }

    public QLearningFacadeConcept(Class<? extends LearningFacadeConcept> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.facade = inits.isInitialized("facade") ? new QLearningFacade(forProperty("facade"), inits.get("facade")) : null;
    }

}

