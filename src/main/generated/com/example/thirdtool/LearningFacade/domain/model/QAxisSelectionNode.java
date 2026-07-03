package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAxisSelectionNode is a Querydsl query type for AxisSelectionNode
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAxisSelectionNode extends EntityPathBase<AxisSelectionNode> {

    private static final long serialVersionUID = -255408428L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAxisSelectionNode axisSelectionNode = new QAxisSelectionNode("axisSelectionNode");

    public final StringPath body = createString("body");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> displayOrder = createNumber("displayOrder", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath rationale = createString("rationale");

    public final QAxisSelection selection;

    public final StringPath title = createString("title");

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public QAxisSelectionNode(String variable) {
        this(AxisSelectionNode.class, forVariable(variable), INITS);
    }

    public QAxisSelectionNode(Path<? extends AxisSelectionNode> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAxisSelectionNode(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAxisSelectionNode(PathMetadata metadata, PathInits inits) {
        this(AxisSelectionNode.class, metadata, inits);
    }

    public QAxisSelectionNode(Class<? extends AxisSelectionNode> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.selection = inits.isInitialized("selection") ? new QAxisSelection(forProperty("selection"), inits.get("selection")) : null;
    }

}

