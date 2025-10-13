package com.example.thirdtool.Scoring.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSm2LearningProfile is a Querydsl query type for Sm2LearningProfile
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSm2LearningProfile extends EntityPathBase<Sm2LearningProfile> {

    private static final long serialVersionUID = 1108823299L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSm2LearningProfile sm2LearningProfile = new QSm2LearningProfile("sm2LearningProfile");

    public final QLearningProfile _super;

    //inherited
    public final NumberPath<Integer> badCount;

    // inherited
    public final com.example.thirdtool.Card.domain.model.QCard card;

    public final NumberPath<Double> easinessFactor = createNumber("easinessFactor", Double.class);

    //inherited
    public final NumberPath<Integer> goodCount;

    //inherited
    public final NumberPath<Integer> greatCount;

    //inherited
    public final NumberPath<Long> id;

    //inherited
    public final EnumPath<com.example.thirdtool.Deck.domain.model.DeckMode> mode;

    //inherited
    public final NumberPath<Integer> normalCount;

    public final NumberPath<Integer> repetition = createNumber("repetition", Integer.class);

    //inherited
    public final NumberPath<Integer> score;

    public QSm2LearningProfile(String variable) {
        this(Sm2LearningProfile.class, forVariable(variable), INITS);
    }

    public QSm2LearningProfile(Path<? extends Sm2LearningProfile> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSm2LearningProfile(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSm2LearningProfile(PathMetadata metadata, PathInits inits) {
        this(Sm2LearningProfile.class, metadata, inits);
    }

    public QSm2LearningProfile(Class<? extends Sm2LearningProfile> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this._super = new QLearningProfile(type, metadata, inits);
        this.badCount = _super.badCount;
        this.card = _super.card;
        this.goodCount = _super.goodCount;
        this.greatCount = _super.greatCount;
        this.id = _super.id;
        this.mode = _super.mode;
        this.normalCount = _super.normalCount;
        this.score = _super.score;
    }

}

