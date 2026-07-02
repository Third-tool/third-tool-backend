package com.example.thirdtool.LearningFacade.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * {@link LearningFacade#updateConcepts(List)}의 결과 VO.
 *
 * <p>이전/현재 값 리스트와 변화 분류(added/removed/kept)를 반환한다.
 * 순서만 바뀌어도 {@link #isChanged()}는 true — 리스트 순서 자체가 도메인 의미이기 때문.
 *
 * <p>Story-LT-E1-S3.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class ConceptsChangeRecord {

    private final List<String> previous;
    private final List<String> current;
    private final List<String> added;
    private final List<String> removed;
    private final List<String> kept;

    static ConceptsChangeRecord of(
            List<String> previous,
            List<String> current,
            List<String> added,
            List<String> removed,
            List<String> kept
    ) {
        return new ConceptsChangeRecord(
                Collections.unmodifiableList(previous),
                Collections.unmodifiableList(current),
                Collections.unmodifiableList(added),
                Collections.unmodifiableList(removed),
                Collections.unmodifiableList(kept)
        );
    }

    public boolean isChanged() {
        return !previous.equals(current);
    }
}
