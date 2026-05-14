package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTopicDeletionRecord is a Querydsl query type for TopicDeletionRecord
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTopicDeletionRecord extends EntityPathBase<TopicDeletionRecord> {

    private static final long serialVersionUID = -98782635L;

    public static final QTopicDeletionRecord topicDeletionRecord = new QTopicDeletionRecord("topicDeletionRecord");

    public final DateTimePath<java.time.LocalDateTime> deletedAt = createDateTime("deletedAt", java.time.LocalDateTime.class);

    public final StringPath description = createString("description");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> learningAxisId = createNumber("learningAxisId", Long.class);

    public final StringPath name = createString("name");

    public final NumberPath<Long> originalTopicId = createNumber("originalTopicId", Long.class);

    public final NumberPath<Integer> revisionCount = createNumber("revisionCount", Integer.class);

    public QTopicDeletionRecord(String variable) {
        super(TopicDeletionRecord.class, forVariable(variable));
    }

    public QTopicDeletionRecord(Path<? extends TopicDeletionRecord> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTopicDeletionRecord(PathMetadata metadata) {
        super(TopicDeletionRecord.class, metadata);
    }

}

