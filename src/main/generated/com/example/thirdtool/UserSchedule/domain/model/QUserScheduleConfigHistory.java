package com.example.thirdtool.UserSchedule.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserScheduleConfigHistory is a Querydsl query type for UserScheduleConfigHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserScheduleConfigHistory extends EntityPathBase<UserScheduleConfigHistory> {

    private static final long serialVersionUID = -1923960467L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserScheduleConfigHistory userScheduleConfigHistory = new QUserScheduleConfigHistory("userScheduleConfigHistory");

    public final DateTimePath<java.time.LocalDateTime> changedAt = createDateTime("changedAt", java.time.LocalDateTime.class);

    public final EnumPath<LearningMode> fromMode = createEnum("fromMode", LearningMode.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> rawInputDays = createNumber("rawInputDays", Integer.class);

    public final EnumPath<LearningMode> toMode = createEnum("toMode", LearningMode.class);

    public final QUserScheduleConfig userScheduleConfig;

    public QUserScheduleConfigHistory(String variable) {
        this(UserScheduleConfigHistory.class, forVariable(variable), INITS);
    }

    public QUserScheduleConfigHistory(Path<? extends UserScheduleConfigHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserScheduleConfigHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserScheduleConfigHistory(PathMetadata metadata, PathInits inits) {
        this(UserScheduleConfigHistory.class, metadata, inits);
    }

    public QUserScheduleConfigHistory(Class<? extends UserScheduleConfigHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.userScheduleConfig = inits.isInitialized("userScheduleConfig") ? new QUserScheduleConfig(forProperty("userScheduleConfig")) : null;
    }

}

