package com.example.thirdtool.LearningFacade.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionChangeRecord {

    private final boolean changed;
    private final String  previousDescription;
    private final String  newDescription;

    static ActionChangeRecord changed(String previousDescription, String newDescription) {
        return new ActionChangeRecord(true, previousDescription, newDescription);
    }

    static ActionChangeRecord unchanged(String description) {
        return new ActionChangeRecord(false, description, description);
    }

    // ─── 행위 ─────────────────────────────────────────────

    public boolean isChanged() {
        return changed;
    }
}