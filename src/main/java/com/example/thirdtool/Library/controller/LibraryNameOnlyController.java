package com.example.thirdtool.Library.controller;

import com.example.thirdtool.Library.application.service.LibraryNameOnlyService;
import com.example.thirdtool.Library.domain.dto.LibraryNameOnlyResponse;
import com.example.thirdtool.User.domain.model.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// library/api/LibraryNameOnlyController.java
@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryNameOnlyController {

    private final LibraryNameOnlyService service;

    // 라이브러리 피드 (덱 이름 목록)
    @GetMapping("/feed")
    public ResponseEntity<Page<LibraryNameOnlyResponse>> feed(
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable
                                                             ) {
        return ResponseEntity.ok(service.feed(pageable));
    }

    // 내 덱 이름을 라이브러리에 등록
    @PostMapping("/decks/{deckId}/publish")
    public ResponseEntity<Long> publish(@AuthenticationPrincipal UserEntity user,
                                        @PathVariable Long deckId) {
        return ResponseEntity.ok(service.publish(user.getId(), deckId));
    }

    // 등록 취소
    @DeleteMapping("/decks/{deckId}/publish")
    public ResponseEntity<Void> unpublish(@AuthenticationPrincipal UserEntity user,
                                          @PathVariable Long deckId) {
        service.unpublish(user.getId(), deckId);
        return ResponseEntity.noContent().build();
    }
}
