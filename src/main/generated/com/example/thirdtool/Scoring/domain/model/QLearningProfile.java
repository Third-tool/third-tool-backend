package com.example.thirdtool.Scoring.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLearningProfile is a Querydsl query type for LearningProfile
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLearningProfile extends EntityPathBase<LearningProfile> {

    private static final long serialVersionUID = 1784464923L;

    public static final QLearningProfile learningProfile = new QLearningProfile("learningProfile");

    public final NumberPath<Integer> badCount = createNumber("badCount", Integer.class);

    public final NumberPath<Card> card = createNumber("card", Card.class);

    public final NumberPath<Integer> goodCount = createNumber("goodCount", Integer.class);

    public final NumberPath<Integer> greatCount = createNumber("greatCount", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<com.example.thirdtool.Deck.domain.model.DeckMode> mode = createEnum("mode", com.example.thirdtool.Deck.domain.model.DeckMode.class);

    public final NumberPath<Integer> normalCount = createNumber("normalCount", Integer.class);

    public final NumberPath<Integer> score = createNumber("score", Integer.class);

    public QLearningProfile(String variable) {
        super(LearningProfile.class, forVariable(variable));
    }

    public QLearningProfile(Path<? extends LearningProfile> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLearningProfile(PathMetadata metadata) {
        super(LearningProfile.class, metadata);
    }

}

