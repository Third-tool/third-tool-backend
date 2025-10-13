package com.example.thirdtool.Card.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCardImage is a Querydsl query type for CardImage
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCardImage extends EntityPathBase<CardImage> {

    private static final long serialVersionUID = 1600620986L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCardImage cardImage = new QCardImage("cardImage");

    public final com.example.thirdtool.Common.QBaseEntity _super = new com.example.thirdtool.Common.QBaseEntity(this);

    public final QCard card;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdDate = _super.createdDate;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<ImageType> imageType = createEnum("imageType", ImageType.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final NumberPath<Integer> sequence = createNumber("sequence", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedDate = _super.updatedDate;

    public QCardImage(String variable) {
        this(CardImage.class, forVariable(variable), INITS);
    }

    public QCardImage(Path<? extends CardImage> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCardImage(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCardImage(PathMetadata metadata, PathInits inits) {
        this(CardImage.class, metadata, inits);
    }

    public QCardImage(Class<? extends CardImage> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.card = inits.isInitialized("card") ? new QCard(forProperty("card"), inits.get("card")) : null;
    }

}

