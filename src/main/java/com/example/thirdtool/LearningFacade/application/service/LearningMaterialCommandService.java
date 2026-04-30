package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import com.example.thirdtool.LearningFacade.domain.model.*;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningFacadeRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.LearningMaterialRepository;
import com.example.thirdtool.LearningFacade.infrastructure.persistence.TopicMaterialRepository;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningMaterialResponse.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LearningMaterialCommandService {

    private final LearningFacadeRepository facadeRepository;
    private final LearningMaterialRepository materialRepository;
    private final TopicMaterialRepository topicMaterialRepository;
    private final CoverageRecalculator coverageRecalculator;

    // ──────────────────────────────────────────────────────
    // 12. 학습 자료 등록
    // ──────────────────────────────────────────────────────

    @Transactional
    public CreateMaterial createMaterial(Long userId, String name, MaterialType materialType,
                                         String url, List<Long> linkedTopicIds) {
        LearningFacade facade = loadFacade(userId);
        LearningMaterial material = LearningMaterial.create(facade, name, materialType, url);
        materialRepository.save(material);

        if (linkedTopicIds != null && !linkedTopicIds.isEmpty()) {
            for (Long topicId : linkedTopicIds) {
                AxisTopic topic = findTopicInFacade(facade, topicId);
                TopicMaterial mapping = TopicMaterial.create(topic, material);
                topicMaterialRepository.save(mapping);
                coverageRecalculator.recalculate(topic);
            }
        }

        LearningMaterial saved = materialRepository.findById(material.getId())
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_MATERIAL_NOT_FOUND));
        return CreateMaterial.of(saved);
    }

    // ──────────────────────────────────────────────────────
    // 13. 학습 자료 목록 조회
    // ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MaterialSummary> getMaterials(Long userId) {
        LearningFacade facade = loadFacade(userId);
        return materialRepository.findByFacadeId(facade.getId())
                .stream()
                .map(MaterialSummary::of)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────
    // 14. 자료 이름 수정
    // ──────────────────────────────────────────────────────

    @Transactional
    public UpdateMaterialName updateMaterialName(Long userId, Long materialId, String newName) {
        LearningMaterial material = loadMaterial(materialId, userId);
        material.updateName(newName);
        return UpdateMaterialName.of(material);
    }

    // ──────────────────────────────────────────────────────
    // 15. 숙련도 자가 평가 수정
    // ──────────────────────────────────────────────────────

    @Transactional
    public UpdateProficiency updateProficiency(Long userId, Long materialId,
                                               ProficiencyLevel newLevel) {
        if (newLevel == ProficiencyLevel.UNRATED) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_MATERIAL_PROFICIENCY_UNRATED_NOT_ALLOWED);
        }

        LearningMaterial material = loadMaterial(materialId, userId);
        material.updateProficiencyLevel(newLevel);

        List<TopicCoverageItem> updatedCoverages = material.getTopicMappings().stream()
                .map(mapping -> {
                    AxisTopic topic = mapping.getTopic();
                    coverageRecalculator.recalculate(topic);
                    return TopicCoverageItem.of(topic);
                })
                .collect(Collectors.toList());

        return UpdateProficiency.of(material, updatedCoverages);
    }

    // ──────────────────────────────────────────────────────
    // 16. 주제-자료 연결 추가
    // ──────────────────────────────────────────────────────

    @Transactional
    public LinkedTopics linkTopic(Long userId, Long materialId, Long topicId) {
        LearningMaterial material = loadMaterial(materialId, userId);

        if (topicMaterialRepository.existsByTopicIdAndMaterialId(topicId, materialId)) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_TOPIC_MATERIAL_ALREADY_LINKED);
        }

        LearningFacade facade = loadFacade(userId);
        AxisTopic topic = findTopicInFacade(facade, topicId);

        TopicMaterial mapping = TopicMaterial.create(topic, material);
        topicMaterialRepository.save(mapping);
        coverageRecalculator.recalculate(topic);

        LearningMaterial reloaded = materialRepository.findById(materialId)
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_MATERIAL_NOT_FOUND));
        return LinkedTopics.of(reloaded);
    }

    // ──────────────────────────────────────────────────────
    // 17. 주제-자료 연결 해제
    // ──────────────────────────────────────────────────────

    @Transactional
    public UnlinkTopic unlinkTopic(Long userId, Long materialId, Long topicId) {
        loadMaterial(materialId, userId);

        TopicMaterial mapping = topicMaterialRepository
                .findByTopicIdAndMaterialId(topicId, materialId)
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.LEARNING_TOPIC_MATERIAL_NOT_LINKED));

        AxisTopic topic = mapping.getTopic();
        topicMaterialRepository.delete(mapping);

        coverageRecalculator.recalculate(topic);

        List<TopicMaterial> remaining = topicMaterialRepository.findByMaterialId(materialId);
        List<LinkedTopicSummary> remainingTopics = remaining.stream()
                .map(LinkedTopicSummary::of)
                .collect(Collectors.toList());

        return new UnlinkTopic(
                materialId,
                topicId,
                TopicCoverageItem.of(topic),
                remainingTopics
        );
    }

    // ──────────────────────────────────────────────────────
    // 18. 학습 자료 삭제
    // ──────────────────────────────────────────────────────

    @Transactional
    public void deleteMaterial(Long userId, Long materialId) {
        LearningMaterial material = loadMaterial(materialId, userId);

        List<AxisTopic> affectedTopics = new ArrayList<>(material.getTopicMappings().stream()
                .map(TopicMaterial::getTopic)
                .toList());

        materialRepository.delete(material);
        affectedTopics.forEach(coverageRecalculator::recalculate);
    }

    // ──────────────────────────────────────────────────────
    // 내부 유틸
    // ──────────────────────────────────────────────────────

    private LearningFacade loadFacade(Long userId) {
        return facadeRepository.findByUserId(userId)
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_NOT_FOUND));
    }

    private LearningMaterial loadMaterial(Long materialId, Long userId) {
        LearningMaterial material = materialRepository.findById(materialId)
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_MATERIAL_NOT_FOUND));
        if (!material.getFacade().getUser().getId().equals(userId)) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_FACADE_NOT_FOUND);
        }
        return material;
    }

    private AxisTopic findTopicInFacade(LearningFacade facade, Long topicId) {
        return facade.getAxes().stream()
                .flatMap(axis -> axis.getTopics().stream())
                .filter(t -> t.getId().equals(topicId))
                .findFirst()
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.LEARNING_AXIS_TOPIC_NOT_FOUND));
    }
}
