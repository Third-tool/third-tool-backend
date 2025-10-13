package com.example.thirdtool.User.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSocialMember is a Querydsl query type for SocialMember
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSocialMember extends EntityPathBase<SocialMember> {

    private static final long serialVersionUID = -938822285L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSocialMember socialMember = new QSocialMember("socialMember");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath refreshToken = createString("refreshToken");

    public final StringPath socialId = createString("socialId");

    public final QUserEntity user;

    public QSocialMember(String variable) {
        this(SocialMember.class, forVariable(variable), INITS);
    }

    public QSocialMember(Path<? extends SocialMember> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSocialMember(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSocialMember(PathMetadata metadata, PathInits inits) {
        this(SocialMember.class, metadata, inits);
    }

    public QSocialMember(Class<? extends SocialMember> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUserEntity(forProperty("user")) : null;
    }

}

