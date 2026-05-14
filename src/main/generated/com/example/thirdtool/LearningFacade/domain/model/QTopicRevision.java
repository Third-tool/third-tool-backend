package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTopicRevision is a Querydsl query type for TopicRevision
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTopicRevision extends EntityPathBase<TopicRevision> {

    private static final long serialVersionUID = 1190947057L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTopicRevision topicRevision = new QTopicRevision("topicRevision");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath newName = createString("newName");

    public final StringPath previousName = createString("previousName");

    public final DateTimePath<java.time.LocalDateTime> revisedAt = createDateTime("revisedAt", java.time.LocalDateTime.class);

    public final StringPath revisionReasonLabel = createString("revisionReasonLabel");

    public final QAxisTopic topic;

    public QTopicRevision(String variable) {
        this(TopicRevision.class, forVariable(variable), INITS);
    }

    public QTopicRevision(Path<? extends TopicRevision> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTopicRevision(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTopicRevision(PathMetadata metadata, PathInits inits) {
        this(TopicRevision.class, metadata, inits);
    }

    public QTopicRevision(Class<? extends TopicRevision> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.topic = inits.isInitialized("topic") ? new QAxisTopic(forProperty("topic"), inits.get("topic")) : null;
    }

}

