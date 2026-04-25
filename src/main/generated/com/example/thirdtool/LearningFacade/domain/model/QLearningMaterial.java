package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLearningMaterial is a Querydsl query type for LearningMaterial
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLearningMaterial extends EntityPathBase<LearningMaterial> {

    private static final long serialVersionUID = -404826562L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLearningMaterial learningMaterial = new QLearningMaterial("learningMaterial");

    public final ListPath<ActionMaterial, QActionMaterial> actionMappings = this.<ActionMaterial, QActionMaterial>createList("actionMappings", ActionMaterial.class, QActionMaterial.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final QLearningFacade facade;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<MaterialType> materialType = createEnum("materialType", MaterialType.class);

    public final StringPath name = createString("name");

    public final EnumPath<ProficiencyLevel> proficiencyLevel = createEnum("proficiencyLevel", ProficiencyLevel.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final StringPath url = createString("url");

    public QLearningMaterial(String variable) {
        this(LearningMaterial.class, forVariable(variable), INITS);
    }

    public QLearningMaterial(Path<? extends LearningMaterial> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLearningMaterial(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLearningMaterial(PathMetadata metadata, PathInits inits) {
        this(LearningMaterial.class, metadata, inits);
    }

    public QLearningMaterial(Class<? extends LearningMaterial> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.facade = inits.isInitialized("facade") ? new QLearningFacade(forProperty("facade"), inits.get("facade")) : null;
    }

}

