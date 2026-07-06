package com.example.thirdtool.Review.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRecommendation is a Querydsl query type for Recommendation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRecommendation extends EntityPathBase<Recommendation> {

    private static final long serialVersionUID = 1264480210L;

    public static final QRecommendation recommendation = new QRecommendation("recommendation");

    public final EnumPath<RecommendationAction> action = createEnum("action", RecommendationAction.class);

    public final EnumPath<com.example.thirdtool.UserSchedule.domain.model.LearningMode> fromMode = createEnum("fromMode", com.example.thirdtool.UserSchedule.domain.model.LearningMode.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath reason = createString("reason");

    public final DateTimePath<java.time.LocalDateTime> resolvedAt = createDateTime("resolvedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.example.thirdtool.UserSchedule.domain.model.LearningMode> toMode = createEnum("toMode", com.example.thirdtool.UserSchedule.domain.model.LearningMode.class);

    public final DateTimePath<java.time.LocalDateTime> triggeredAt = createDateTime("triggeredAt", java.time.LocalDateTime.class);

    public final EnumPath<RecommendationType> type = createEnum("type", RecommendationType.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QRecommendation(String variable) {
        super(Recommendation.class, forVariable(variable));
    }

    public QRecommendation(Path<? extends Recommendation> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRecommendation(PathMetadata metadata) {
        super(Recommendation.class, metadata);
    }

}

