package com.example.thirdtool.Review.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDailyLearningBatch is a Querydsl query type for DailyLearningBatch
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDailyLearningBatch extends EntityPathBase<DailyLearningBatch> {

    private static final long serialVersionUID = 2057630908L;

    public static final QDailyLearningBatch dailyLearningBatch = new QDailyLearningBatch("dailyLearningBatch");

    public final DatePath<java.time.LocalDate> batchDate = createDate("batchDate", java.time.LocalDate.class);

    public final DateTimePath<java.time.LocalDateTime> closedAt = createDateTime("closedAt", java.time.LocalDateTime.class);

    public final ListPath<DailyCardEntry, QDailyCardEntry> entries = this.<DailyCardEntry, QDailyCardEntry>createList("entries", DailyCardEntry.class, QDailyCardEntry.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> generatedAt = createDateTime("generatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public final EnumPath<com.example.thirdtool.UserSchedule.domain.model.LearningMode> userModeAtGeneration = createEnum("userModeAtGeneration", com.example.thirdtool.UserSchedule.domain.model.LearningMode.class);

    public QDailyLearningBatch(String variable) {
        super(DailyLearningBatch.class, forVariable(variable));
    }

    public QDailyLearningBatch(Path<? extends DailyLearningBatch> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDailyLearningBatch(PathMetadata metadata) {
        super(DailyLearningBatch.class, metadata);
    }

}

