package com.example.thirdtool.User.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserEntity is a Querydsl query type for UserEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserEntity extends EntityPathBase<UserEntity> {

    private static final long serialVersionUID = -672172134L;

    public static final QUserEntity userEntity = new QUserEntity("userEntity");

    public final com.example.thirdtool.Common.QBaseEntity _super = new com.example.thirdtool.Common.QBaseEntity(this);

    public final ListPath<com.example.thirdtool.Card.domain.model.CardRank, com.example.thirdtool.Card.domain.model.QCardRank> cardRanks = this.<com.example.thirdtool.Card.domain.model.CardRank, com.example.thirdtool.Card.domain.model.QCardRank>createList("cardRanks", com.example.thirdtool.Card.domain.model.CardRank.class, com.example.thirdtool.Card.domain.model.QCardRank.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> createdDate = createDateTime("createdDate", java.time.LocalDateTime.class);

    public final ListPath<com.example.thirdtool.Deck.domain.model.Deck, com.example.thirdtool.Deck.domain.model.QDeck> decks = this.<com.example.thirdtool.Deck.domain.model.Deck, com.example.thirdtool.Deck.domain.model.QDeck>createList("decks", com.example.thirdtool.Deck.domain.model.Deck.class, com.example.thirdtool.Deck.domain.model.QDeck.class, PathInits.DIRECT2);

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isLock = createBoolean("isLock");

    public final BooleanPath isSocial = createBoolean("isSocial");

    public final ListPath<com.example.thirdtool.User.infrastructure.kakao.KakaoMember, com.example.thirdtool.User.infrastructure.kakao.QKakaoMember> kakaoMembers = this.<com.example.thirdtool.User.infrastructure.kakao.KakaoMember, com.example.thirdtool.User.infrastructure.kakao.QKakaoMember>createList("kakaoMembers", com.example.thirdtool.User.infrastructure.kakao.KakaoMember.class, com.example.thirdtool.User.infrastructure.kakao.QKakaoMember.class, PathInits.DIRECT2);

    public final ListPath<com.example.thirdtool.User.infrastructure.Naver.NaverMember, com.example.thirdtool.User.infrastructure.Naver.QNaverMember> naverMembers = this.<com.example.thirdtool.User.infrastructure.Naver.NaverMember, com.example.thirdtool.User.infrastructure.Naver.QNaverMember>createList("naverMembers", com.example.thirdtool.User.infrastructure.Naver.NaverMember.class, com.example.thirdtool.User.infrastructure.Naver.QNaverMember.class, PathInits.DIRECT2);

    public final StringPath nickname = createString("nickname");

    public final StringPath password = createString("password");

    public final EnumPath<UserRoleType> roleType = createEnum("roleType", UserRoleType.class);

    public final EnumPath<SocialProviderType> socialProviderType = createEnum("socialProviderType", SocialProviderType.class);

    public final DateTimePath<java.time.LocalDateTime> updatedDate = createDateTime("updatedDate", java.time.LocalDateTime.class);

    public final StringPath username = createString("username");

    public QUserEntity(String variable) {
        super(UserEntity.class, forVariable(variable));
    }

    public QUserEntity(Path<? extends UserEntity> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUserEntity(PathMetadata metadata) {
        super(UserEntity.class, metadata);
    }

}

