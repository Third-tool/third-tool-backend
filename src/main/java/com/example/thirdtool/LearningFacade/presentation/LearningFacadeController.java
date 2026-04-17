package com.example.thirdtool.LearningFacade.presentation;

import com.example.thirdtool.LearningFacade.application.service.LearningFacadeCommandService;
import com.example.thirdtool.LearningFacade.application.service.LearningFacadeQueryService;
import com.example.thirdtool.LearningFacade.application.service.LearningMaterialCommandService;
import com.example.thirdtool.LearningFacade.domain.model.MaterialType;
import com.example.thirdtool.LearningFacade.domain.model.ProficiencyLevel;
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

    // ──────────────────────────────────────────────────────
    // 1. LearningFacade 생성
    // ──────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LearningFacadeResponse.CreateFacade createFacade(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningFacadeRequest.CreateFacade request
                                                           ) {
        return facadeCommandService.createFacade(user, request.concept());
    }

    // ──────────────────────────────────────────────────────
    // 2. LearningFacade 단건 조회
    // ──────────────────────────────────────────────────────

    @GetMapping
    public LearningFacadeResponse.FacadeDetail getFacade(
            @AuthenticationPrincipal UserEntity user
                                                        ) {
        return facadeQueryService.getFacade(user.getId());
    }

    // ──────────────────────────────────────────────────────
    // 3. 컨셉 수정
    // ──────────────────────────────────────────────────────

    @PatchMapping("/concept")
    public LearningFacadeResponse.UpdateConcept updateConcept(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningFacadeRequest.UpdateConcept request
                                                             ) {
        return facadeCommandService.updateConcept(user, request.concept());
    }

    // ──────────────────────────────────────────────────────
    // 4. 축 추가
    // ──────────────────────────────────────────────────────

    @PostMapping("/axes")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningFacadeResponse.AddAxis addAxis(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningFacadeRequest.AddAxis request
                                                 ) {
        return facadeCommandService.addAxis(user, request.name());
    }

    // ──────────────────────────────────────────────────────
    // 5. 축 이름 수정
    // ──────────────────────────────────────────────────────

    @PatchMapping("/axes/{axisId}")
    public LearningFacadeResponse.UpdateAxisName updateAxisName(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @Valid @RequestBody LearningFacadeRequest.UpdateAxisName request
                                                               ) {
        return facadeCommandService.updateAxisName(user, axisId, request.name());
    }

    // ──────────────────────────────────────────────────────
    // 6. 축 삭제
    // ──────────────────────────────────────────────────────

    @DeleteMapping("/axes/{axisId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAxis(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId
                          ) {
        facadeCommandService.removeAxis(user, axisId);
    }

    // ──────────────────────────────────────────────────────
    // 7. 축 순서 변경
    // ──────────────────────────────────────────────────────

    @PutMapping("/axes/order")
    public LearningFacadeResponse.ReorderAxes reorderAxes(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody LearningFacadeRequest.ReorderAxes request
                                                         ) {
        return facadeCommandService.reorderAxes(user, request.orderedAxisIds());
    }

    // ──────────────────────────────────────────────────────
    // 8. 행동 추가
    // ──────────────────────────────────────────────────────

    @PostMapping("/axes/{axisId}/actions")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningFacadeResponse.AddAction addAction(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @Valid @RequestBody LearningFacadeRequest.AddAction request
                                                     ) {
        return facadeCommandService.addAction(user, axisId, request.description());
    }

    // ──────────────────────────────────────────────────────
    // 9. 행동 동사 수정
    // ──────────────────────────────────────────────────────

    @PatchMapping("/axes/{axisId}/actions/{actionId}")
    public LearningFacadeResponse.UpdateAction updateAction(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @PathVariable Long actionId,
            @Valid @RequestBody LearningFacadeRequest.UpdateAction request
                                                           ) {
        return facadeCommandService.updateAction(
                user, axisId, actionId,
                request.description(), request.revisionReasonLabel()
                                                );
    }

    // ──────────────────────────────────────────────────────
    // 10. 행동 삭제
    // ──────────────────────────────────────────────────────

    @DeleteMapping("/axes/{axisId}/actions/{actionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAction(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @PathVariable Long actionId
                            ) {
        facadeCommandService.removeAction(user, axisId, actionId);
    }

    // ──────────────────────────────────────────────────────
    // 11. 행동 수정 이력 조회
    // ──────────────────────────────────────────────────────

    @GetMapping("/actions/{actionId}/revisions")
    public List<LearningFacadeResponse.RevisionItem> getRevisions(
            @PathVariable Long actionId
                                                                 ) {
        return facadeQueryService.getRevisions(actionId);
    }

    // ──────────────────────────────────────────────────────
    // 12. 수정 이유 선택지 목록 조회
    // ──────────────────────────────────────────────────────

    @GetMapping("/revision-reason-options")
    public List<LearningFacadeResponse.RevisionReasonOptionItem> getRevisionReasonOptions() {
        return facadeQueryService.getRevisionReasonOptions();
    }

    // ──────────────────────────────────────────────────────
    // 13. 학습 자료 등록
    // ──────────────────────────────────────────────────────

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
                request.linkedActionIds()
                                                    );
    }

    // ──────────────────────────────────────────────────────
    // 14. 학습 자료 목록 조회
    // ──────────────────────────────────────────────────────

    @GetMapping("/materials")
    public List<LearningMaterialResponse.MaterialSummary> getMaterials(
            @AuthenticationPrincipal UserEntity user
                                                                      ) {
        return materialCommandService.getMaterials(user.getId());
    }

    // ──────────────────────────────────────────────────────
    // 15. 자료 이름 수정
    // ──────────────────────────────────────────────────────

    @PatchMapping("/materials/{materialId}/name")
    public LearningMaterialResponse.UpdateMaterialName updateMaterialName(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId,
            @Valid @RequestBody LearningMaterialRequest.UpdateMaterialName request
                                                                         ) {
        return materialCommandService.updateMaterialName(user.getId(), materialId, request.name());
    }

    // ──────────────────────────────────────────────────────
    // 16. 숙련도 자가 평가 수정
    // ──────────────────────────────────────────────────────

    @PatchMapping("/materials/{materialId}/proficiency")
    public LearningMaterialResponse.UpdateProficiency updateProficiency(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId,
            @Valid @RequestBody LearningMaterialRequest.UpdateProficiency request
                                                                       ) {
        return materialCommandService.updateProficiency(
                user.getId(), materialId, request.proficiencyLevel());
    }

    // ──────────────────────────────────────────────────────
    // 17. 행동-자료 연결 추가
    // ──────────────────────────────────────────────────────

    @PostMapping("/materials/{materialId}/actions")
    @ResponseStatus(HttpStatus.CREATED)
    public LearningMaterialResponse.LinkedActions linkAction(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId,
            @Valid @RequestBody LearningMaterialRequest.LinkAction request
                                                            ) {
        return materialCommandService.linkAction(user.getId(), materialId, request.actionId());
    }

    // ──────────────────────────────────────────────────────
    // 18. 행동-자료 연결 해제
    // ──────────────────────────────────────────────────────

    @DeleteMapping("/materials/{materialId}/actions/{actionId}")
    public LearningMaterialResponse.UnlinkAction unlinkAction(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId,
            @PathVariable Long actionId
                                                             ) {
        return materialCommandService.unlinkAction(user.getId(), materialId, actionId);
    }

    // ──────────────────────────────────────────────────────
    // 19. 학습 자료 삭제
    // ──────────────────────────────────────────────────────

    @DeleteMapping("/materials/{materialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMaterial(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long materialId
                              ) {
        materialCommandService.deleteMaterial(user.getId(), materialId);
    }
}