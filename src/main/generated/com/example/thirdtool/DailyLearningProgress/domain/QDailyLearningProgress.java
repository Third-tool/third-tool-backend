package com.example.thirdtool.DailyLearningProgress.domain;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QDailyLearningProgress is a Querydsl query type for DailyLearningProgress
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDailyLearningProgress extends EntityPathBase<DailyLearningProgress> {

    private static final long serialVersionUID = 640975140L;

    public static final QDailyLearningProgress dailyLearningProgress = new QDailyLearningProgress("dailyLearningProgress");

    public final NumberPath<Integer> diamondCount = createNumber("diamondCount", Integer.class);

    public final NumberPath<Integer> goldCount = createNumber("goldCount", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Integer> silverCount = createNumber("silverCount", Integer.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QDailyLearningProgress(String variable) {
        super(DailyLearningProgress.class, forVariable(variable));
    }

    public QDailyLearningProgress(Path<? extends DailyLearningProgress> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDailyLearningProgress(PathMetadata metadata) {
        super(DailyLearningProgress.class, metadata);
    }

}

