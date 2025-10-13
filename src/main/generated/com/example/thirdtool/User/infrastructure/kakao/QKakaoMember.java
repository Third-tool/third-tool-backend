package com.example.thirdtool.User.infrastructure.kakao;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QKakaoMember is a Querydsl query type for KakaoMember
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QKakaoMember extends EntityPathBase<KakaoMember> {

    private static final long serialVersionUID = 2006662506L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QKakaoMember kakaoMember = new QKakaoMember("kakaoMember");

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

    public QKakaoMember(String variable) {
        this(KakaoMember.class, forVariable(variable), INITS);
    }

    public QKakaoMember(Path<? extends KakaoMember> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QKakaoMember(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QKakaoMember(PathMetadata metadata, PathInits inits) {
        this(KakaoMember.class, metadata, inits);
    }

    public QKakaoMember(Class<? extends KakaoMember> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this._super = new com.example.thirdtool.User.domain.model.QSocialMember(type, metadata, inits);
        this.id = _super.id;
        this.refreshToken = _super.refreshToken;
        this.socialId = _super.socialId;
        this.user = _super.user;
    }

}

