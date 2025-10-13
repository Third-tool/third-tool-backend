package com.example.thirdtool.Scoring.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QLearningProfile is a Querydsl query type for LearningProfile
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLearningProfile extends EntityPathBase<LearningProfile> {

    private static final long serialVersionUID = 1784464923L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QLearningProfile learningProfile = new QLearningProfile("learningProfile");

    public final NumberPath<Integer> badCount = createNumber("badCount", Integer.class);

    public final com.example.thirdtool.Card.domain.model.QCard card;

    public final NumberPath<Integer> goodCount = createNumber("goodCount", Integer.class);

    public final NumberPath<Integer> greatCount = createNumber("greatCount", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<com.example.thirdtool.Deck.domain.model.DeckMode> mode = createEnum("mode", com.example.thirdtool.Deck.domain.model.DeckMode.class);

    public final NumberPath<Integer> normalCount = createNumber("normalCount", Integer.class);

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    public QLearningProfile(String variable) {
        this(LearningProfile.class, forVariable(variable), INITS);
    }

    public QLearningProfile(Path<? extends LearningProfile> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QLearningProfile(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QLearningProfile(PathMetadata metadata, PathInits inits) {
        this(LearningProfile.class, metadata, inits);
    }

    public QLearningProfile(Class<? extends LearningProfile> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.card = inits.isInitialized("card") ? new com.example.thirdtool.Card.domain.model.QCard(forProperty("card"), inits.get("card")) : null;
    }

}

