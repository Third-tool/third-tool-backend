package com.example.thirdtool.User.infrastructure.Naver;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNaverMember is a Querydsl query type for NaverMember
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNaverMember extends EntityPathBase<NaverMember> {

    private static final long serialVersionUID = -543087452L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNaverMember naverMember = new QNaverMember("naverMember");

    public final com.example.thirdtool.User.domain.model.QSocialMember _super;

    //inherited
    public final NumberPath<Long> id;

    public final EnumPath<com.example.thirdtool.User.domain.model.SocialProviderType> providerType = createEnum("providerType", com.example.thirdtool.User.domain.model.SocialProviderType.class);

    //inherited
    public final StringPath refreshToken;

    //inherited
    public final StringPath socialId;

    // inherited
    public final com.example.thirdtool.User.domain.model.QUserEntity user;

    public QNaverMember(String variable) {
        this(NaverMember.class, forVariable(variable), INITS);
    }

    public QNaverMember(Path<? extends NaverMember> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QNaverMember(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QNaverMember(PathMetadata metadata, PathInits inits) {
        this(NaverMember.class, metadata, inits);
    }

    public QNaverMember(Class<? extends NaverMember> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this._super = new com.example.thirdtool.User.domain.model.QSocialMember(type, metadata, inits);
        this.id = _super.id;
        this.refreshToken = _super.refreshToken;
        this.socialId = _super.socialId;
        this.user = _super.user;
    }

}

