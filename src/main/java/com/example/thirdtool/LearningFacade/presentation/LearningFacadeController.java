package com.example.thirdtool.LearningFacade.presentation;

import com.example.thirdtool.LearningFacade.application.service.LearningFacadeCommandService;
import com.example.thirdtool.LearningFacade.application.service.LearningFacadeQueryService;
import com.example.thirdtool.LearningFacade.application.service.LearningMaterialCommandService;
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

    // 1. POST /learning-facade
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LearningFacadeResponse.CreateFacade createFacade(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningFacadeRequest.CreateFacade request
    ) {
        return facadeCommandService.createFacade(user, request.concept());
    }

    // 2. GET /learning-facade
    @GetMapping
    public LearningFacadeResponse.FacadeDetail getFacade(
            @AuthenticationPrincipal UserEntity user
    ) {
        return facadeQueryService.getFacade(user.getId());
    }

    // 3. PATCH /learning-facade/concept
    @PatchMapping("/concept")
    public LearningFacadeResponse.UpdateConcept updateConcept(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningFacadeRequest.UpdateConcept request
    ) {
        return facadeCommandService.updateConcept(user, request.concept());
    }

    // 4. POST /learning-facade/axes
    @PostMapping("/axes")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningFacadeResponse.AddAxis addAxis(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningFacadeRequest.AddAxis request
    ) {
        return facadeCommandService.addAxis(user, request.name());
    }

    // 5. PATCH /learning-facade/axes/{axisId}
    @PatchMapping("/axes/{axisId}")
    public LearningFacadeResponse.UpdateAxisName updateAxisName(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @Valid @RequestBody LearningFacadeRequest.UpdateAxisName request
    ) {
        return facadeCommandService.updateAxisName(user, axisId, request.name());
    }

    // 6. DELETE /learning-facade/axes/{axisId}
    @DeleteMapping("/axes/{axisId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAxis(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId
    ) {
        facadeCommandService.removeAxis(user, axisId);
    }

    // 7. PUT /learning-facade/axes/order
    @PutMapping("/axes/order")
    public LearningFacadeResponse.ReorderAxes reorderAxes(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningFacadeRequest.ReorderAxes request
    ) {
        return facadeCommandService.reorderAxes(user, request.orderedAxisIds());
    }

    // 8. POST /learning-facade/axes/{axisId}/topics
    @PostMapping("/axes/{axisId}/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningFacadeResponse.AddTopic addTopic(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @Valid @RequestBody LearningFacadeRequest.AddTopic request
    ) {
        return facadeCommandService.addTopic(user, axisId, request.name(), request.description());
    }

    // 9. PATCH /learning-facade/axes/{axisId}/topics/{topicId}
    @PatchMapping("/axes/{axisId}/topics/{topicId}")
    public LearningFacadeResponse.UpdateTopic updateTopic(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @PathVariable Long topicId,
            @RequestBody LearningFacadeRequest.UpdateTopic request
    ) {
        return facadeCommandService.updateTopic(user, axisId, topicId, request);
    }

    // 10. DELETE /learning-facade/axes/{axisId}/topics/{topicId}
    @DeleteMapping("/axes/{axisId}/topics/{topicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTopic(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @PathVariable Long topicId
    ) {
        facadeCommandService.removeTopic(user, axisId, topicId);
    }

    // 11. PUT /learning-facade/axes/{axisId}/topics/order
    @PutMapping("/axes/{axisId}/topics/order")
    public LearningFacadeResponse.ReorderTopics reorderTopics(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @Valid @RequestBody LearningFacadeRequest.ReorderTopics request
    ) {
        return facadeCommandService.reorderTopics(user, axisId, request.orderedTopicIds());
    }

    // 12. POST /learning-facade/materials
    @PostMapping("/materials")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningMaterialResponse.CreateMaterial createMaterial(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningMaterialRequest.CreateMaterial request
    ) {
        return materialCommandService.createMaterial(
                user.getId(),
                request.name(),
                request.materialType(),
                request.url(),
                request.linkedTopicIds()
        );
    }

    // 13. GET /learning-facade/materials
    @GetMapping("/materials")
    public List<LearningMaterialResponse.MaterialSummary> getMaterials(
            @AuthenticationPrincipal UserEntity user
    ) {
        return materialCommandService.getMaterials(user.getId());
    }

    // 14. PATCH /learning-facade/materials/{materialId}/name
    @PatchMapping("/materials/{materialId}/name")
    public LearningMaterialResponse.UpdateMaterialName updateMaterialName(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId,
            @Valid @RequestBody LearningMaterialRequest.UpdateMaterialName request
    ) {
        return materialCommandService.updateMaterialName(user.getId(), materialId, request.name());
    }

    // 15. PATCH /learning-facade/materials/{materialId}/proficiency
    @PatchMapping("/materials/{materialId}/proficiency")
    public LearningMaterialResponse.UpdateProficiency updateProficiency(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId,
            @Valid @RequestBody LearningMaterialRequest.UpdateProficiency request
    ) {
        return materialCommandService.updateProficiency(
                user.getId(), materialId, request.proficiencyLevel());
    }

    // 16. POST /learning-facade/materials/{materialId}/topics
    @PostMapping("/materials/{materialId}/topics")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningMaterialResponse.LinkedTopics linkTopic(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId,
            @Valid @RequestBody LearningMaterialRequest.LinkTopic request
    ) {
        return materialCommandService.linkTopic(user.getId(), materialId, request.topicId());
    }

    // 17. DELETE /learning-facade/materials/{materialId}/topics/{topicId}
    @DeleteMapping("/materials/{materialId}/topics/{topicId}")
    public LearningMaterialResponse.UnlinkTopic unlinkTopic(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId,
            @PathVariable Long topicId
    ) {
        return materialCommandService.unlinkTopic(user.getId(), materialId, topicId);
    }

    // 18. DELETE /learning-facade/materials/{materialId}
    @DeleteMapping("/materials/{materialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMaterial(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId
    ) {
        materialCommandService.deleteMaterial(user.getId(), materialId);
    }
}
