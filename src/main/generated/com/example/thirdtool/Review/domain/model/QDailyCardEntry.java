package com.example.thirdtool.Review.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDailyCardEntry is a Querydsl query type for DailyCardEntry
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDailyCardEntry extends EntityPathBase<DailyCardEntry> {

    private static final long serialVersionUID = -110410430L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDailyCardEntry dailyCardEntry = new QDailyCardEntry("dailyCardEntry");

    public final QDailyLearningBatch batch;

    public final NumberPath<Long> cardId = createNumber("cardId", Long.class);

    public final NumberPath<Integer> cardIntervalDay = createNumber("cardIntervalDay", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> exposedAt = createDateTime("exposedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> viewedAt = createDateTime("viewedAt", java.time.LocalDateTime.class);

    public QDailyCardEntry(String variable) {
        this(DailyCardEntry.class, forVariable(variable), INITS);
    }

    public QDailyCardEntry(Path<? extends DailyCardEntry> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDailyCardEntry(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDailyCardEntry(PathMetadata metadata, PathInits inits) {
        this(DailyCardEntry.class, metadata, inits);
    }

    public QDailyCardEntry(Class<? extends DailyCardEntry> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.batch = inits.isInitialized("batch") ? new QDailyLearningBatch(forProperty("batch")) : null;
    }

}

