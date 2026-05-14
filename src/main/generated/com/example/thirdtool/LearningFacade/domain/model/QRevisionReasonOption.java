package com.example.thirdtool.LearningFacade.domain.model;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QRevisionReasonOption is a Querydsl query type for RevisionReasonOption
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRevisionReasonOption extends EntityPathBase<RevisionReasonOption> {

    private static final long serialVersionUID = 1106010797L;

    public static final QRevisionReasonOption revisionReasonOption = new QRevisionReasonOption("revisionReasonOption");

    public final BooleanPath active = createBoolean("active");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> displayOrder = createNumber("displayOrder", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath label = createString("label");

    public QRevisionReasonOption(String variable) {
        super(RevisionReasonOption.class, forVariable(variable));
    }

    public QRevisionReasonOption(Path<? extends RevisionReasonOption> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRevisionReasonOption(PathMetadata metadata) {
        super(RevisionReasonOption.class, metadata);
    }

}

