package com.example.thirdtool.UserSchedule.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserScheduleConfig is a Querydsl query type for UserScheduleConfig
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserScheduleConfig extends EntityPathBase<UserScheduleConfig> {

    private static final long serialVersionUID = 1712357671L;

    public static final QUserScheduleConfig userScheduleConfig = new QUserScheduleConfig("userScheduleConfig");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<LearningMode> mappedMode = createEnum("mappedMode", LearningMode.class);

    public final NumberPath<Integer> rawInputDays = createNumber("rawInputDays", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedAt = createDateTime("updatedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QUserScheduleConfig(String variable) {
        super(UserScheduleConfig.class, forVariable(variable));
    }

    public QUserScheduleConfig(Path<? extends UserScheduleConfig> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserScheduleConfig(PathMetadata metadata) {
        super(UserScheduleConfig.class, metadata);
    }

}

