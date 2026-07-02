package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.AxisSelection;
import com.example.thirdtool.LearningFacade.domain.model.LearningAxis;
import com.example.thirdtool.LearningFacade.domain.model.LearningFacade;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.AxisSelectionRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.AxisSelectionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Selection 컨테이너 + 자식 노드 Query Application Service (Story-LT-E3-S3-11).
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AxisSelectionQueryService {

    private final LearningFacadeRepository facadeRepository;
    private final AxisSelectionRepository selectionRepository;

    /**
     * axis 하위 활성 Selection 컨테이너 리스트 (created_at DESC).
     */
    public AxisSelectionResponse.SelectionList listSelections(Long userId, Long axisId) {
        LearningFacade facade = facadeRepository.findByUserId(userId)
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_NOT_FOUND));
        LearningAxis axis = facade.getAxes().stream()
                .filter(a -> a.getId() != null && a.getId().equals(axisId))
                .findFirst()
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_NOT_FOUND));

        return AxisSelectionResponse.SelectionList.of(
                selectionRepository.findByAxisIdOrderByCreatedAtDesc(axis.getId()));
    }

    /**
     * Selection 컨테이너 하위 자식 노드 리스트 (display_order ASC).
     */
    public AxisSelectionResponse.NodeList listNodes(Long userId, Long selectionId) {
        LearningFacade facade = facadeRepository.findByUserId(userId)
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_NOT_FOUND));
        AxisSelection selection = selectionRepository.findById(selectionId)
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.AXIS_SELECTION_NOT_FOUND));

        // 소유권 검증
        LearningAxis axis = selection.getAxis();
        if (axis == null || axis.getFacade() == null
                || !axis.getFacade().getId().equals(facade.getId())) {
            throw LearningFacadeDomainException.of(ErrorCode.AXIS_SELECTION_NOT_FOUND);
        }

        return AxisSelectionResponse.NodeList.of(selection.getNodes());
    }
}
