package com.example.thirdtool.Review.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUserNotification is a Querydsl query type for UserNotification
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserNotification extends EntityPathBase<UserNotification> {

    private static final long serialVersionUID = 188546607L;

    public static final QUserNotification userNotification = new QUserNotification("userNotification");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath payloadJson = createString("payloadJson");

    public final DateTimePath<java.time.LocalDateTime> readAt = createDateTime("readAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> recommendationId = createNumber("recommendationId", Long.class);

    public final EnumPath<NotificationType> type = createEnum("type", NotificationType.class);

    public final NumberPath<Long> userId = createNumber("userId", Long.class);

    public QUserNotification(String variable) {
        super(UserNotification.class, forVariable(variable));
    }

    public QUserNotification(Path<? extends UserNotification> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserNotification(PathMetadata metadata) {
        super(UserNotification.class, metadata);
    }

}

