package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAxisRoadmapNode is a Querydsl query type for AxisRoadmapNode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAxisRoadmapNode extends EntityPathBase<AxisRoadmapNode> {

    private static final long serialVersionUID = 1708450052L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAxisRoadmapNode axisRoadmapNode = new QAxisRoadmapNode("axisRoadmapNode");

    public final QLearningAxis axis;

    public final StringPath body = createString("body");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> displayOrder = createNumber("displayOrder", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath rationale = createString("rationale");

    public final StringPath title = createString("title");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QAxisRoadmapNode(String variable) {
        this(AxisRoadmapNode.class, forVariable(variable), INITS);
    }

    public QAxisRoadmapNode(Path<? extends AxisRoadmapNode> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAxisRoadmapNode(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAxisRoadmapNode(PathMetadata metadata, PathInits inits) {
        this(AxisRoadmapNode.class, metadata, inits);
    }

    public QAxisRoadmapNode(Class<? extends AxisRoadmapNode> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.axis = inits.isInitialized("axis") ? new QLearningAxis(forProperty("axis"), inits.get("axis")) : null;
    }

}

