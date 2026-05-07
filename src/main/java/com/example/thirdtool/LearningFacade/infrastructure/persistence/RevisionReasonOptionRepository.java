package com.example.thirdtool.LearningFacade.infrastructure.persistence;

import com.example.thirdtool.LearningFacade.domain.model.RevisionReasonOption;

import java.util.List;
import java.util.Optional;

public interface RevisionReasonOptionRepository {

    Optional<RevisionReasonOption> findById(Long id);

    /** 활성 선택지를 displayOrder 오름차순으로 반환. 신규 수정 입력 시 노출 목록. */
    List<RevisionReasonOption> findActiveOrderByDisplayOrderAsc();

    /** 활성 + 해당 id의 선택지만 조회. 비활성·미존재 모두 empty. */
    Optional<RevisionReasonOption> findActiveById(Long id);
}
