package com.example.thirdtool.User.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = -703863593L;

    public static final QUser user = new QUser("user");

    public final ListPath<com.example.thirdtool.Deck.domain.model.Deck, com.example.thirdtool.Deck.domain.model.QDeck> decks = this.<com.example.thirdtool.Deck.domain.model.Deck, com.example.thirdtool.Deck.domain.model.QDeck>createList("decks", com.example.thirdtool.Deck.domain.model.Deck.class, com.example.thirdtool.Deck.domain.model.QDeck.class, PathInits.DIRECT2);

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath password = createString("password");

    public final ListPath<com.example.thirdtool.Tag.domain.model.Tag, com.example.thirdtool.Tag.domain.model.QTag> tags = this.<com.example.thirdtool.Tag.domain.model.Tag, com.example.thirdtool.Tag.domain.model.QTag>createList("tags", com.example.thirdtool.Tag.domain.model.Tag.class, com.example.thirdtool.Tag.domain.model.QTag.class, PathInits.DIRECT2);

    public QUser(String variable) {
        super(User.class, forVariable(variable));
    }

    public QUser(Path<? extends User> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUser(PathMetadata metadata) {
        super(User.class, metadata);
    }

}

