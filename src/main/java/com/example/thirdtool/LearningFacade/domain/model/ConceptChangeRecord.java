package com.example.thirdtool.LearningFacade.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ConceptChangeRecord {

    private final boolean changed;
    private final String  previousConcept;
    private final String  newConcept;

    // ─── 정적 팩토리 ──────────────────────────────────────
    static ConceptChangeRecord changed(String previousConcept, String newConcept) {
        return new ConceptChangeRecord(true, previousConcept, newConcept);
    }

    static ConceptChangeRecord unchanged(String concept) {
        return new ConceptChangeRecord(false, concept, concept);
    }

    // ─── 행위 ─────────────────────────────────────────────

    public boolean isDrifted() {
        return changed;
    }
}
