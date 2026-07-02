package com.example.thirdtool.LearningFacade.presentation;

import com.example.thirdtool.LearningFacade.application.dto.AxisSelectionCommand;
import com.example.thirdtool.LearningFacade.application.service.AxisSelectionCommandService;
import com.example.thirdtool.LearningFacade.application.service.AxisSelectionQueryService;
import com.example.thirdtool.LearningFacade.presentation.dto.AxisSelectionRequest;
import com.example.thirdtool.LearningFacade.presentation.dto.AxisSelectionResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Selection 컨테이너 + 자식 노드 REST Controller (Story-LT-E3-S3-11, 이슈 #16 · 이슈 #11 계승).
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AxisSelectionController {

    private final AxisSelectionCommandService commandService;
    private final AxisSelectionQueryService queryService;

    // ─── 컨테이너 CRUD ─────────────────────────────

    // POST /api/v1/axes/{axisId}/selections
    @PostMapping("/axes/{axisId}/selections")
    public ResponseEntity<AxisSelectionResponse.SelectionDetail> addSelection(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @Valid @RequestBody AxisSelectionRequest.AddSelection request
    ) {
        AxisSelectionResponse.SelectionDetail detail = commandService.addSelection(
                new AxisSelectionCommand.AddSelection(user.getId(), axisId, request.name()));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/selections/{selectionId}")
                .buildAndExpand(detail.selectionId())
                .toUri();
        return ResponseEntity.created(location).body(detail);
    }

    // GET /api/v1/axes/{axisId}/selections
    @GetMapping("/axes/{axisId}/selections")
    public AxisSelectionResponse.SelectionList listSelections(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId
    ) {
        return queryService.listSelections(user.getId(), axisId);
    }

    // PATCH /api/v1/selections/{selectionId}
    @PatchMapping("/selections/{selectionId}")
    public AxisSelectionResponse.SelectionDetail renameSelection(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long selectionId,
            @Valid @RequestBody AxisSelectionRequest.RenameSelection request
    ) {
        return commandService.renameSelection(
                new AxisSelectionCommand.RenameSelection(user.getId(), selectionId, request.name()));
    }

    // DELETE /api/v1/axes/{axisId}/selections/{selectionId}
    // NOTE: SDD Story 3-11는 이 URL(axisId 포함)로 컨테이너 hard delete 명세. axisId는 검증에만 사용.
    @DeleteMapping("/axes/{axisId}/selections/{selectionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeSelection(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @PathVariable Long selectionId
    ) {
        // axisId는 client convenience — 소유권 검증은 selectionId → axis → facade 체인으로 이미 처리됨.
        commandService.removeSelection(
                new AxisSelectionCommand.RemoveSelection(user.getId(), selectionId));
    }

    // ─── 자식 노드 CRUD ────────────────────────────

    // POST /api/v1/selections/{selectionId}/nodes
    @PostMapping("/selections/{selectionId}/nodes")
    public ResponseEntity<AxisSelectionResponse.NodeDetail> addNode(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long selectionId,
            @Valid @RequestBody AxisSelectionRequest.AddNode request
    ) {
        AxisSelectionResponse.NodeDetail detail = commandService.addNode(
                new AxisSelectionCommand.AddNode(
                        user.getId(), selectionId, request.title(), request.rationale(), request.body()));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/selection-nodes/{nodeId}")
                .buildAndExpand(detail.nodeId())
                .toUri();
        return ResponseEntity.created(location).body(detail);
    }

    // GET /api/v1/selections/{selectionId}/nodes
    @GetMapping("/selections/{selectionId}/nodes")
    public AxisSelectionResponse.NodeList listNodes(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long selectionId
    ) {
        return queryService.listNodes(user.getId(), selectionId);
    }

    // PATCH /api/v1/selection-nodes/{nodeId}
    @PatchMapping("/selection-nodes/{nodeId}")
    public AxisSelectionResponse.NodeDetail updateNode(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long nodeId,
            @Valid @RequestBody AxisSelectionRequest.UpdateNode request
    ) {
        return commandService.updateNode(new AxisSelectionCommand.UpdateNode(
                user.getId(),
                nodeId,
                request.title(),
                request.rationale(),
                request.body(),
                request.title() != null,
                request.rationale() != null,
                request.body() != null
        ));
    }

    // DELETE /api/v1/selection-nodes/{nodeId}
    @DeleteMapping("/selection-nodes/{nodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeNode(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long nodeId
    ) {
        commandService.removeNode(new AxisSelectionCommand.RemoveNode(user.getId(), nodeId));
    }

    // PUT /api/v1/selections/{selectionId}/nodes/order
    @PutMapping("/selections/{selectionId}/nodes/order")
    public AxisSelectionResponse.Reordered reorderNodes(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long selectionId,
            @Valid @RequestBody AxisSelectionRequest.ReorderNodes request
    ) {
        return commandService.reorderNodes(new AxisSelectionCommand.ReorderNodes(
                user.getId(), selectionId, request.orderedNodeIds()));
    }
}
