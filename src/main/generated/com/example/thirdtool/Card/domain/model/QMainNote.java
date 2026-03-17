package com.example.thirdtool.Card.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMainNote is a Querydsl query type for MainNote
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QMainNote extends BeanPath<MainNote> {

    private static final long serialVersionUID = -779692580L;

    public static final QMainNote mainNote = new QMainNote("mainNote");

    public final EnumPath<MainContentType> contentType = createEnum("contentType", MainContentType.class);

    public final StringPath imageUrl = createString("imageUrl");

    public final StringPath textContent = createString("textContent");

    public QMainNote(String variable) {
        super(MainNote.class, forVariable(variable));
    }

    public QMainNote(Path<? extends MainNote> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMainNote(PathMetadata metadata) {
        super(MainNote.class, metadata);
    }

}

