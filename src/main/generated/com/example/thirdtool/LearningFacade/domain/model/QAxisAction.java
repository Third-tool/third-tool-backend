package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAxisAction is a Querydsl query type for AxisAction
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAxisAction extends EntityPathBase<AxisAction> {

    private static final long serialVersionUID = -2078276656L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAxisAction axisAction = new QAxisAction("axisAction");

    public final QLearningAxis axis;

    public final EnumPath<CoverageStatus> coverageStatus = createEnum("coverageStatus", CoverageStatus.class);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> revisionCount = createNumber("revisionCount", Integer.class);

    public final ListPath<ActionRevision, QActionRevision> revisions = this.<ActionRevision, QActionRevision>createList("revisions", ActionRevision.class, QActionRevision.class, PathInits.DIRECT2);

    public QAxisAction(String variable) {
        this(AxisAction.class, forVariable(variable), INITS);
    }

    public QAxisAction(Path<? extends AxisAction> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAxisAction(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAxisAction(PathMetadata metadata, PathInits inits) {
        this(AxisAction.class, metadata, inits);
    }

    public QAxisAction(Class<? extends AxisAction> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.axis = inits.isInitialized("axis") ? new QLearningAxis(forProperty("axis"), inits.get("axis")) : null;
    }

}

