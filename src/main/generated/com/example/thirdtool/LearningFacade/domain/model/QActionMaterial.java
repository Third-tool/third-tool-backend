package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QActionMaterial is a Querydsl query type for ActionMaterial
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QActionMaterial extends EntityPathBase<ActionMaterial> {

    private static final long serialVersionUID = 1856990102L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QActionMaterial actionMaterial = new QActionMaterial("actionMaterial");

    public final QAxisAction action;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> linkedAt = createDateTime("linkedAt", java.time.LocalDateTime.class);

    public final QLearningMaterial material;

    public QActionMaterial(String variable) {
        this(ActionMaterial.class, forVariable(variable), INITS);
    }

    public QActionMaterial(Path<? extends ActionMaterial> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QActionMaterial(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QActionMaterial(PathMetadata metadata, PathInits inits) {
        this(ActionMaterial.class, metadata, inits);
    }

    public QActionMaterial(Class<? extends ActionMaterial> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.action = inits.isInitialized("action") ? new QAxisAction(forProperty("action"), inits.get("action")) : null;
        this.material = inits.isInitialized("material") ? new QLearningMaterial(forProperty("material"), inits.get("material")) : null;
    }

}

