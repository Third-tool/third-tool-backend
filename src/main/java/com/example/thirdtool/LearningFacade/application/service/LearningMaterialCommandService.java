package com.example.thirdtool.LearningFacade.application.service;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.application.dto.LearningMaterialCommand;
import com.example.thirdtool.LearningFacade.application.dto.LearningMaterialQuery;
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
    public CreateMaterial createMaterial(LearningMaterialCommand.CreateMaterial command) {
        MaterialType materialType = parseMaterialType(command.materialType());
        LearningFacade facade = loadFacade(command.userId());
        LearningMaterial material = LearningMaterial.create(
                facade,
                command.name(),
                materialType,
                command.url(),
                command.author(),
                command.platform(),
                command.aiProvider(),
                command.webSource(),
                command.memo());
        materialRepository.save(material);

        List<Long> linkedTopicIds = command.linkedTopicIds();
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

    private MaterialType parseMaterialType(String raw) {
        if (raw == null || raw.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_MATERIAL_TYPE_REQUIRED);
        }
        try {
            return MaterialType.valueOf(raw.trim());
        } catch (IllegalArgumentException e) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_MATERIAL_TYPE_INVALID);
        }
    }

    // ──────────────────────────────────────────────────────
    // 13. 학습 자료 목록 조회 (Query)
    // ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MaterialSummary> getMaterials(LearningMaterialQuery.GetMaterials query) {
        LearningFacade facade = loadFacade(query.userId());
        return materialRepository.findByFacadeId(facade.getId())
                .stream()
                .map(MaterialSummary::of)
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────
    // 14. 자료 이름 수정
    // ──────────────────────────────────────────────────────

    @Transactional
    public UpdateMaterialName updateMaterialName(LearningMaterialCommand.UpdateMaterialName command) {
        LearningMaterial material = loadMaterial(command.materialId(), command.userId());
        material.updateName(command.name());
        return UpdateMaterialName.of(material);
    }

    // ──────────────────────────────────────────────────────
    // 15. 숙련도 자가 평가 수정
    // ──────────────────────────────────────────────────────

    @Transactional
    public UpdateProficiency updateProficiency(LearningMaterialCommand.UpdateProficiency command) {
        if (command.proficiencyLevel() == ProficiencyLevel.UNRATED) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_MATERIAL_PROFICIENCY_UNRATED_NOT_ALLOWED);
        }

        LearningMaterial material = loadMaterial(command.materialId(), command.userId());
        material.updateProficiencyLevel(command.proficiencyLevel());

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
    public LinkedTopics linkTopic(LearningMaterialCommand.LinkTopic command) {
        LearningMaterial material = loadMaterial(command.materialId(), command.userId());

        if (topicMaterialRepository.existsByTopicIdAndMaterialId(command.topicId(), command.materialId())) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_TOPIC_MATERIAL_ALREADY_LINKED);
        }

        LearningFacade facade = loadFacade(command.userId());
        AxisTopic topic = findTopicInFacade(facade, command.topicId());

        TopicMaterial mapping = TopicMaterial.create(topic, material);
        topicMaterialRepository.save(mapping);
        coverageRecalculator.recalculate(topic);

        LearningMaterial reloaded = materialRepository.findById(command.materialId())
                .orElseThrow(() -> LearningFacadeDomainException.of(ErrorCode.LEARNING_MATERIAL_NOT_FOUND));
        return LinkedTopics.of(reloaded);
    }

    // ──────────────────────────────────────────────────────
    // 17. 주제-자료 연결 해제
    // ──────────────────────────────────────────────────────

    @Transactional
    public UnlinkTopic unlinkTopic(LearningMaterialCommand.UnlinkTopic command) {
        loadMaterial(command.materialId(), command.userId());

        TopicMaterial mapping = topicMaterialRepository
                .findByTopicIdAndMaterialId(command.topicId(), command.materialId())
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.LEARNING_TOPIC_MATERIAL_NOT_LINKED));

        AxisTopic topic = mapping.getTopic();
        topicMaterialRepository.delete(mapping);

        coverageRecalculator.recalculate(topic);

        List<TopicMaterial> remaining = topicMaterialRepository.findByMaterialId(command.materialId());
        List<LinkedTopicSummary> remainingTopics = remaining.stream()
                .map(LinkedTopicSummary::of)
                .collect(Collectors.toList());

        return new UnlinkTopic(
                command.materialId(),
                command.topicId(),
                TopicCoverageItem.of(topic),
                remainingTopics
        );
    }

    // ──────────────────────────────────────────────────────
    // 18. 학습 자료 삭제
    // ──────────────────────────────────────────────────────

    @Transactional
    public void deleteMaterial(LearningMaterialCommand.DeleteMaterial command) {
        LearningMaterial material = loadMaterial(command.materialId(), command.userId());

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
