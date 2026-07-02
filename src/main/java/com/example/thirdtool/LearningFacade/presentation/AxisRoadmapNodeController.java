package com.example.thirdtool.LearningFacade.presentation;

import com.example.thirdtool.LearningFacade.application.dto.AxisRoadmapNodeCommand;
import com.example.thirdtool.LearningFacade.application.service.AxisRoadmapNodeCommandService;
import com.example.thirdtool.LearningFacade.application.service.AxisRoadmapNodeQueryService;
import com.example.thirdtool.LearningFacade.presentation.dto.AxisRoadmapNodeRequest;
import com.example.thirdtool.LearningFacade.presentation.dto.AxisRoadmapNodeResponse;
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
 * Roadmap 노드 REST Controller (Story-LT-E3-S3-8, 이슈 #15).
 *
 * <p>SDD (`product-learning-tower.md` line 1434~1438) 명세 대로 top-level `/api/v1/axes/{axisId}/roadmap-nodes`
 * 리소스로 노출. 기존 `PUT /api/v1/axes/{axisId}/roadmap {content}` 엔드포인트는 존재한 적 없어 410 Gone 처리 불필요.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AxisRoadmapNodeController {

    private final AxisRoadmapNodeCommandService commandService;
    private final AxisRoadmapNodeQueryService queryService;

    // POST /api/v1/axes/{axisId}/roadmap-nodes
    @PostMapping("/axes/{axisId}/roadmap-nodes")
    public ResponseEntity<AxisRoadmapNodeResponse.NodeDetail> add(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @Valid @RequestBody AxisRoadmapNodeRequest.Add request
    ) {
        AxisRoadmapNodeResponse.NodeDetail detail = commandService.add(
                new AxisRoadmapNodeCommand.Add(
                        user.getId(), axisId, request.title(), request.rationale(), request.body()));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/roadmap-nodes/{nodeId}")
                .buildAndExpand(detail.nodeId())
                .toUri();
        return ResponseEntity.created(location).body(detail);
    }

    // GET /api/v1/axes/{axisId}/roadmap-nodes
    @GetMapping("/axes/{axisId}/roadmap-nodes")
    public AxisRoadmapNodeResponse.NodeList list(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId
    ) {
        return queryService.list(user.getId(), axisId);
    }

    // PATCH /api/v1/roadmap-nodes/{nodeId}
    @PatchMapping("/roadmap-nodes/{nodeId}")
    public AxisRoadmapNodeResponse.NodeDetail update(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long nodeId,
            @Valid @RequestBody AxisRoadmapNodeRequest.Update request
    ) {
        return commandService.update(new AxisRoadmapNodeCommand.Update(
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

    // DELETE /api/v1/roadmap-nodes/{nodeId}
    @DeleteMapping("/roadmap-nodes/{nodeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long nodeId
    ) {
        commandService.remove(new AxisRoadmapNodeCommand.Remove(user.getId(), nodeId));
    }

    // PUT /api/v1/axes/{axisId}/roadmap-nodes/order
    @PutMapping("/axes/{axisId}/roadmap-nodes/order")
    public AxisRoadmapNodeResponse.Reordered reorder(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long axisId,
            @Valid @RequestBody AxisRoadmapNodeRequest.Reorder request
    ) {
        return commandService.reorder(new AxisRoadmapNodeCommand.Reorder(
                user.getId(), axisId, request.orderedNodeIds()));
    }
}
