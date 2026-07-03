package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAxisSelection is a Querydsl query type for AxisSelection
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAxisSelection extends EntityPathBase<AxisSelection> {

    private static final long serialVersionUID = 394513842L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAxisSelection axisSelection = new QAxisSelection("axisSelection");

    public final QLearningAxis axis;

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath name = createString("name");

    public final ListPath<AxisSelectionNode, QAxisSelectionNode> nodes = this.<AxisSelectionNode, QAxisSelectionNode>createList("nodes", AxisSelectionNode.class, QAxisSelectionNode.class, PathInits.DIRECT2);

    public QAxisSelection(String variable) {
        this(AxisSelection.class, forVariable(variable), INITS);
    }

    public QAxisSelection(Path<? extends AxisSelection> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAxisSelection(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAxisSelection(PathMetadata metadata, PathInits inits) {
        this(AxisSelection.class, metadata, inits);
    }

    public QAxisSelection(Class<? extends AxisSelection> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.axis = inits.isInitialized("axis") ? new QLearningAxis(forProperty("axis"), inits.get("axis")) : null;
    }

}

