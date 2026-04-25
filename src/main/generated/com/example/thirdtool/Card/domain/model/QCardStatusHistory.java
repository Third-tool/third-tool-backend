package com.example.thirdtool.Card.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCardStatusHistory is a Querydsl query type for CardStatusHistory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCardStatusHistory extends EntityPathBase<CardStatusHistory> {

    private static final long serialVersionUID = -652064351L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCardStatusHistory cardStatusHistory = new QCardStatusHistory("cardStatusHistory");

    public final QCard card;

    public final DateTimePath<java.time.LocalDateTime> changedAt = createDateTime("changedAt", java.time.LocalDateTime.class);

    public final EnumPath<CardStatus> fromStatus = createEnum("fromStatus", CardStatus.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<ArchiveReason> reason = createEnum("reason", ArchiveReason.class);

    public final EnumPath<CardStatus> toStatus = createEnum("toStatus", CardStatus.class);

    public QCardStatusHistory(String variable) {
        this(CardStatusHistory.class, forVariable(variable), INITS);
    }

    public QCardStatusHistory(Path<? extends CardStatusHistory> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCardStatusHistory(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCardStatusHistory(PathMetadata metadata, PathInits inits) {
        this(CardStatusHistory.class, metadata, inits);
    }

    public QCardStatusHistory(Class<? extends CardStatusHistory> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.card = inits.isInitialized("card") ? new QCard(forProperty("card"), inits.get("card")) : null;
    }

}

