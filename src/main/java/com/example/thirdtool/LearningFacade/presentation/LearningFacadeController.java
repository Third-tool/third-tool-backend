package com.example.thirdtool.LearningFacade.presentation;

import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeCommand;
import com.example.thirdtool.LearningFacade.application.dto.LearningFacadeQuery;
import com.example.thirdtool.LearningFacade.application.dto.LearningMaterialCommand;
import com.example.thirdtool.LearningFacade.application.dto.LearningMaterialQuery;
import com.example.thirdtool.LearningFacade.application.service.LearningFacadeCommandService;
import com.example.thirdtool.LearningFacade.application.service.LearningFacadeQueryService;
import com.example.thirdtool.LearningFacade.application.service.LearningMaterialCommandService;
import com.example.thirdtool.LearningFacade.application.service.TopicRevisionQueryService;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeRequest;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningFacadeResponse;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningMaterialRequest;
import com.example.thirdtool.LearningFacade.presentation.dto.LearningMaterialResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/learning-facade")
@RequiredArgsConstructor
public class LearningFacadeController {

    private final LearningFacadeCommandService facadeCommandService;
    private final LearningFacadeQueryService  facadeQueryService;
    private final LearningMaterialCommandService materialCommandService;
    private final TopicRevisionQueryService topicRevisionQueryService;

    // 1. POST /learning-facade
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LearningFacadeResponse.CreateFacade createFacade(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningFacadeRequest.CreateFacade request
    ) {
        return facadeCommandService.createFacade(
                new LearningFacadeCommand.CreateFacade(user, request.concept()));
    }

    // 2. GET /learning-facade
    @GetMapping
    public LearningFacadeResponse.FacadeDetail getFacade(
            @AuthenticationPrincipal UserEntity user
    ) {
        return facadeQueryService.getFacade(
                new LearningFacadeQuery.GetFacade(user.getId()));
    }

    // 3. PATCH /learning-facade/concept
    @PatchMapping("/concept")
    public LearningFacadeResponse.UpdateConcept updateConcept(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningFacadeRequest.UpdateConcept request
    ) {
        return facadeCommandService.updateConcept(
                new LearningFacadeCommand.UpdateConcept(user.getId(), request.concept()));
    }

    // 4. POST /learning-facade/axes
    @PostMapping("/axes")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningFacadeResponse.AddAxis addAxis(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningFacadeRequest.AddAxis request
    ) {
        return facadeCommandService.addAxis(
                new LearningFacadeCommand.AddAxis(user.getId(), request.name()));
    }

    // 5. PATCH /learning-facade/axes/{axisId}
    @PatchMapping("/axes/{axisId}")
    public LearningFacadeResponse.UpdateAxisName updateAxisName(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @Valid @RequestBody LearningFacadeRequest.UpdateAxisName request
    ) {
        return facadeCommandService.updateAxisName(
                new LearningFacadeCommand.UpdateAxisName(user.getId(), axisId, request.name()));
    }

    // 6. DELETE /learning-facade/axes/{axisId}
    @DeleteMapping("/axes/{axisId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAxis(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId
    ) {
        facadeCommandService.removeAxis(
                new LearningFacadeCommand.RemoveAxis(user.getId(), axisId));
    }

    // 7. PUT /learning-facade/axes/order
    @PutMapping("/axes/order")
    public LearningFacadeResponse.ReorderAxes reorderAxes(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningFacadeRequest.ReorderAxes request
    ) {
        return facadeCommandService.reorderAxes(
                new LearningFacadeCommand.ReorderAxes(user.getId(), request.orderedAxisIds()));
    }

    // 8. POST /learning-facade/axes/{axisId}/topics
    @PostMapping("/axes/{axisId}/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningFacadeResponse.AddTopic addTopic(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @Valid @RequestBody LearningFacadeRequest.AddTopic request
    ) {
        return facadeCommandService.addTopic(
                new LearningFacadeCommand.AddTopic(
                        user.getId(), axisId, request.name(), request.description()));
    }

    // 9. PATCH /learning-facade/axes/{axisId}/topics/{topicId}
    @PatchMapping("/axes/{axisId}/topics/{topicId}")
    public LearningFacadeResponse.UpdateTopic updateTopic(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @PathVariable Long topicId,
            @RequestBody LearningFacadeRequest.UpdateTopic request
    ) {
        return facadeCommandService.updateTopic(
                new LearningFacadeCommand.UpdateTopic(
                        user.getId(),
                        axisId,
                        topicId,
                        request.getName(),
                        request.getDescription(),
                        request.getRevisionReasonOptionId(),
                        request.isNamePresent(),
                        request.isDescriptionPresent()));
    }

    // 10. DELETE /learning-facade/axes/{axisId}/topics/{topicId}
    @DeleteMapping("/axes/{axisId}/topics/{topicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTopic(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @PathVariable Long topicId
    ) {
        facadeCommandService.removeTopic(
                new LearningFacadeCommand.RemoveTopic(user.getId(), axisId, topicId));
    }

    // 11. PUT /learning-facade/axes/{axisId}/topics/order
    @PutMapping("/axes/{axisId}/topics/order")
    public LearningFacadeResponse.ReorderTopics reorderTopics(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @Valid @RequestBody LearningFacadeRequest.ReorderTopics request
    ) {
        return facadeCommandService.reorderTopics(
                new LearningFacadeCommand.ReorderTopics(
                        user.getId(), axisId, request.orderedTopicIds()));
    }

    // 12. POST /learning-facade/materials
    @PostMapping("/materials")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningMaterialResponse.CreateMaterial createMaterial(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningMaterialRequest.CreateMaterial request
    ) {
        return materialCommandService.createMaterial(
                new LearningMaterialCommand.CreateMaterial(
                        user.getId(),
                        request.name(),
                        request.materialType(),
                        request.url(),
                        request.author(),
                        request.platform(),
                        request.aiProvider(),
                        request.webSource(),
                        request.memo(),
                        request.linkedTopicIds()));
    }

    // 13. GET /learning-facade/materials
    @GetMapping("/materials")
    public List<LearningMaterialResponse.MaterialSummary> getMaterials(
            @AuthenticationPrincipal UserEntity user
    ) {
        return materialCommandService.getMaterials(
                new LearningMaterialQuery.GetMaterials(user.getId()));
    }

    // 14. PATCH /learning-facade/materials/{materialId}/name
    @PatchMapping("/materials/{materialId}/name")
    public LearningMaterialResponse.UpdateMaterialName updateMaterialName(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId,
            @Valid @RequestBody LearningMaterialRequest.UpdateMaterialName request
    ) {
        return materialCommandService.updateMaterialName(
                new LearningMaterialCommand.UpdateMaterialName(
                        user.getId(), materialId, request.name()));
    }

    // 15. PATCH /learning-facade/materials/{materialId}/proficiency
    @PatchMapping("/materials/{materialId}/proficiency")
    public LearningMaterialResponse.UpdateProficiency updateProficiency(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId,
            @Valid @RequestBody LearningMaterialRequest.UpdateProficiency request
    ) {
        return materialCommandService.updateProficiency(
                new LearningMaterialCommand.UpdateProficiency(
                        user.getId(), materialId, request.proficiencyLevel()));
    }

    // 16. POST /learning-facade/materials/{materialId}/topics
    @PostMapping("/materials/{materialId}/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningMaterialResponse.LinkedTopics linkTopic(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId,
            @Valid @RequestBody LearningMaterialRequest.LinkTopic request
    ) {
        return materialCommandService.linkTopic(
                new LearningMaterialCommand.LinkTopic(
                        user.getId(), materialId, request.topicId()));
    }

    // 17. DELETE /learning-facade/materials/{materialId}/topics/{topicId}
    @DeleteMapping("/materials/{materialId}/topics/{topicId}")
    public LearningMaterialResponse.UnlinkTopic unlinkTopic(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId,
            @PathVariable Long topicId
    ) {
        return materialCommandService.unlinkTopic(
                new LearningMaterialCommand.UnlinkTopic(
                        user.getId(), materialId, topicId));
    }

    // 18. DELETE /learning-facade/materials/{materialId}
    @DeleteMapping("/materials/{materialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMaterial(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId
    ) {
        materialCommandService.deleteMaterial(
                new LearningMaterialCommand.DeleteMaterial(user.getId(), materialId));
    }

    // ──────────────────────────────────────────────────────
    // Topic 수정 이력 (Story-003-2)
    // ──────────────────────────────────────────────────────

    // 19. GET /learning-facade/topics/{topicId}/revisions
    @GetMapping("/topics/{topicId}/revisions")
    public LearningFacadeResponse.TopicRevisions getTopicRevisions(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long topicId
    ) {
        return topicRevisionQueryService.getRevisions(
                new LearningFacadeQuery.GetTopicRevisions(topicId));
    }

    // 20. GET /learning-facade/revision-reason-options
    @GetMapping("/revision-reason-options")
    public List<LearningFacadeResponse.RevisionReasonOptionItem> getActiveReasonOptions(
            @AuthenticationPrincipal UserEntity user
    ) {
        return topicRevisionQueryService.getActiveReasonOptions(
                new LearningFacadeQuery.GetActiveReasonOptions());
    }

    // 21. GET /learning-facade/axes/{axisId}/topic-deletions  (Story-003-4)
    @GetMapping("/axes/{axisId}/topic-deletions")
    public LearningFacadeResponse.TopicDeletions getTopicDeletions(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId
    ) {
        return topicRevisionQueryService.getDeletions(
                new LearningFacadeQuery.GetTopicDeletions(axisId));
    }
}
