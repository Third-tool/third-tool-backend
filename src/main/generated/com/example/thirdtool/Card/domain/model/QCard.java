package com.example.thirdtool.Card.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCard is a Querydsl query type for Card
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCard extends EntityPathBase<Card> {

    private static final long serialVersionUID = 29465633L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QCard card = new QCard("card");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<KeywordCue, QKeywordCue> keywordCues = this.<KeywordCue, QKeywordCue>createList("keywordCues", KeywordCue.class, QKeywordCue.class, PathInits.DIRECT2);

    public final QMainNote mainNote;

    public final QSummary summary;

    public QCard(String variable) {
        this(Card.class, forVariable(variable), INITS);
    }

    public QCard(Path<? extends Card> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QCard(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QCard(PathMetadata metadata, PathInits inits) {
        this(Card.class, metadata, inits);
    }

    public QCard(Class<? extends Card> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.mainNote = inits.isInitialized("mainNote") ? new QMainNote(forProperty("mainNote")) : null;
        this.summary = inits.isInitialized("summary") ? new QSummary(forProperty("summary")) : null;
    }

}

