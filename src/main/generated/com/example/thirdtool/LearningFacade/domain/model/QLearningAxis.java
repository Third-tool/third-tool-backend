package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLearningAxis is a Querydsl query type for LearningAxis
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLearningAxis extends EntityPathBase<LearningAxis> {

    private static final long serialVersionUID = -2100069320L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLearningAxis learningAxis = new QLearningAxis("learningAxis");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> displayOrder = createNumber("displayOrder", Integer.class);

    public final QLearningFacade facade;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final ListPath<AxisTopic, QAxisTopic> topics = this.<AxisTopic, QAxisTopic>createList("topics", AxisTopic.class, QAxisTopic.class, PathInits.DIRECT2);

    public QLearningAxis(String variable) {
        this(LearningAxis.class, forVariable(variable), INITS);
    }

    public QLearningAxis(Path<? extends LearningAxis> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLearningAxis(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLearningAxis(PathMetadata metadata, PathInits inits) {
        this(LearningAxis.class, metadata, inits);
    }

    public QLearningAxis(Class<? extends LearningAxis> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.facade = inits.isInitialized("facade") ? new QLearningFacade(forProperty("facade"), inits.get("facade")) : null;
    }

}

