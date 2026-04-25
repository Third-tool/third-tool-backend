package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.ActionRevisionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.RevisionReasonOptionRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningFacadeQueryService {

    private final LearningFacadeRepository facadeRepository;
    private final ActionRevisionRepository revisionRepository;
    private final RevisionReasonOptionRepository reasonOptionRepository;

    // ──────────────────────────────────────────────────────
    // 2. LearningFacade 단건 조회
    // ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public FacadeDetail getFacade(Long userId) {
        LearningFacade facade = facadeRepository.findByUserId(userId)
                                                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_NOT_FOUND));
        return FacadeDetail.of(facade);
    }

    // ──────────────────────────────────────────────────────
    // 11. 행동 수정 이력 조회
    // ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RevisionItem> getRevisions(Long actionId) {
        return revisionRepository.findByActionIdOrderByRevisedAtAsc(actionId)
                                 .stream()
                                 .map(RevisionItem::of)
                                 .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────
    // 12. 수정 이유 선택지 목록 조회
    // ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RevisionReasonOptionItem> getRevisionReasonOptions() {
        return reasonOptionRepository.findAllByActiveTrueOrderByDisplayOrderAsc()
                                     .stream()
                                     .map(RevisionReasonOptionItem::of)
                                     .collect(Collectors.toList());
    }
}