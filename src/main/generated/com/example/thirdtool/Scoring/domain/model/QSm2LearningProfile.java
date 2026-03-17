package com.example.thirdtool.Scoring.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSm2LearningProfile is a Querydsl query type for Sm2LearningProfile
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSm2LearningProfile extends EntityPathBase<Sm2LearningProfile> {

    private static final long serialVersionUID = 1108823299L;

    public static final QSm2LearningProfile sm2LearningProfile = new QSm2LearningProfile("sm2LearningProfile");

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

    public QSm2LearningProfile(String variable) {
        super(Sm2LearningProfile.class, forVariable(variable));
    }

    public QSm2LearningProfile(Path<? extends Sm2LearningProfile> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSm2LearningProfile(PathMetadata metadata) {
        super(Sm2LearningProfile.class, metadata);
    }

}

