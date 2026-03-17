package com.example.thirdtool.Scoring.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLeitnerLearningProfile is a Querydsl query type for LeitnerLearningProfile
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLeitnerLearningProfile extends EntityPathBase<LeitnerLearningProfile> {

    private static final long serialVersionUID = -314412668L;

    public static final QLeitnerLearningProfile leitnerLearningProfile = new QLeitnerLearningProfile("leitnerLearningProfile");

    public final QLearningProfile _super = new QLearningProfile(this);

    public final StringPath algorithmType = createString("algorithmType");

    //inherited
    public final NumberPath<Integer> badCount = _super.badCount;

    //inherited
    public final NumberPath<Card> card = _super.card;

    public final NumberPath<Double> easinessFactor = createNumber("easinessFactor", Double.class);

    //inherited
    public final NumberPath<Integer> goodCount = _super.goodCount;

    //inherited
    public final NumberPath<Integer> greatCount = _super.greatCount;

    //inherited
    public final NumberPath<Long> id = _super.id;

    //inherited
    public final EnumPath<com.example.thirdtool.Deck.domain.model.DeckMode> mode = _super.mode;

    //inherited
    public final NumberPath<Integer> normalCount = _super.normalCount;

    public final NumberPath<Integer> repetition = createNumber("repetition", Integer.class);

    //inherited
    public final NumberPath<Integer> score = _super.score;

    public QLeitnerLearningProfile(String variable) {
        super(LeitnerLearningProfile.class, forVariable(variable));
    }

    public QLeitnerLearningProfile(Path<? extends LeitnerLearningProfile> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLeitnerLearningProfile(PathMetadata metadata) {
        super(LeitnerLearningProfile.class, metadata);
    }

}

