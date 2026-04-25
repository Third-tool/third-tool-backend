package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QActionRevision is a Querydsl query type for ActionRevision
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QActionRevision extends EntityPathBase<ActionRevision> {

    private static final long serialVersionUID = 1297137226L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QActionRevision actionRevision = new QActionRevision("actionRevision");

    public final QAxisAction action;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath newDescription = createString("newDescription");

    public final StringPath previousDescription = createString("previousDescription");

    public final DateTimePath<java.time.LocalDateTime> revisedAt = createDateTime("revisedAt", java.time.LocalDateTime.class);

    public final StringPath revisionReasonLabel = createString("revisionReasonLabel");

    public QActionRevision(String variable) {
        this(ActionRevision.class, forVariable(variable), INITS);
    }

    public QActionRevision(Path<? extends ActionRevision> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QActionRevision(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QActionRevision(PathMetadata metadata, PathInits inits) {
        this(ActionRevision.class, metadata, inits);
    }

    public QActionRevision(Class<? extends ActionRevision> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.action = inits.isInitialized("action") ? new QAxisAction(forProperty("action"), inits.get("action")) : null;
    }

}

