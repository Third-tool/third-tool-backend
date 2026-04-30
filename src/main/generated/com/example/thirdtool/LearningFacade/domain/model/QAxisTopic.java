package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAxisTopic is a Querydsl query type for AxisTopic
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAxisTopic extends EntityPathBase<AxisTopic> {

    private static final long serialVersionUID = -49140651L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAxisTopic axisTopic = new QAxisTopic("axisTopic");

    public final QLearningAxis axis;

    public final EnumPath<CoverageStatus> coverageStatus = createEnum("coverageStatus", CoverageStatus.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final NumberPath<Integer> displayOrder = createNumber("displayOrder", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QAxisTopic(String variable) {
        this(AxisTopic.class, forVariable(variable), INITS);
    }

    public QAxisTopic(Path<? extends AxisTopic> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAxisTopic(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAxisTopic(PathMetadata metadata, PathInits inits) {
        this(AxisTopic.class, metadata, inits);
    }

    public QAxisTopic(Class<? extends AxisTopic> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.axis = inits.isInitialized("axis") ? new QLearningAxis(forProperty("axis"), inits.get("axis")) : null;
    }

}

