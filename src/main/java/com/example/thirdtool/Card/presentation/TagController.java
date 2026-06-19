package com.example.thirdtool.Card.presentation;

import com.example.thirdtool.Card.application.service.TagCommandService;
import com.example.thirdtool.Card.application.service.TagQueryService;
import com.example.thirdtool.Card.presentation.dto.TagResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Tag 관리 화면 진입점 (Story 5-1).
 *
 * <p>카드 상세에서의 Tag 추가/제거는 {@link CardController}의 카드 부속물 엔드포인트가 담당하고,
 * 본 컨트롤러는 사용자 단위 Tag 관리(목록 / 일괄 해제)만 노출한다.
 */
@RestController
@RequiredArgsConstructor
public class TagController {

    private final TagQueryService tagQueryService;
    private final TagCommandService tagCommandService;

    /** GET /api/v1/tags — 본인 Tag 목록 + 연결 카드 수. value 사전순. */
    @GetMapping("/api/v1/tags")
    public ResponseEntity<List<TagResponse.Item>> listMyTags(
            @AuthenticationPrincipal UserEntity user
                                                            ) {
        return ResponseEntity.ok(tagQueryService.listMyTags(user.getId()));
    }

    /** DELETE /api/v1/tags/{tagId} — 본인 카드에서 해당 Tag 일괄 해제. Tag row는 보존. */
    @DeleteMapping("/api/v1/tags/{tagId}")
    public ResponseEntity<Void> detachTag(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable Long tagId
                                         ) {
        tagCommandService.detachTagFromMyCards(tagId, user.getId());
        return ResponseEntity.noContent().build();
    }
}
