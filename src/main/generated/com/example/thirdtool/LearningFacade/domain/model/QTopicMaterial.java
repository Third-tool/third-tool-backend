package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTopicMaterial is a Querydsl query type for TopicMaterial
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTopicMaterial extends EntityPathBase<TopicMaterial> {

    private static final long serialVersionUID = 1750799933L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTopicMaterial topicMaterial = new QTopicMaterial("topicMaterial");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> linkedAt = createDateTime("linkedAt", java.time.LocalDateTime.class);

    public final QLearningMaterial material;

    public final QAxisTopic topic;

    public QTopicMaterial(String variable) {
        this(TopicMaterial.class, forVariable(variable), INITS);
    }

    public QTopicMaterial(Path<? extends TopicMaterial> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTopicMaterial(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTopicMaterial(PathMetadata metadata, PathInits inits) {
        this(TopicMaterial.class, metadata, inits);
    }

    public QTopicMaterial(Class<? extends TopicMaterial> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.material = inits.isInitialized("material") ? new QLearningMaterial(forProperty("material"), inits.get("material")) : null;
        this.topic = inits.isInitialized("topic") ? new QAxisTopic(forProperty("topic"), inits.get("topic")) : null;
    }

}

