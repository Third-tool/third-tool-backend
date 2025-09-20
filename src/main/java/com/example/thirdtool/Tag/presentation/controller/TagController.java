package com.example.thirdtool.Tag.presentation.controller;

import com.example.thirdtool.Tag.application.service.TagService;
import com.example.thirdtool.Tag.domain.model.Tag;
import com.example.thirdtool.Tag.presentation.dto.TagCreateRequestDto;
import com.example.thirdtool.Tag.presentation.dto.TagResponseDto;
import com.example.thirdtool.Tag.presentation.dto.TagUpdateRequestDto;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/tags") // ★ 공통 base 경로
public class TagController {

    private final TagService tagService;

    // 태그 생성 (컬렉션 대상 → id 없음)
    @PostMapping
    public ResponseEntity<TagResponseDto> createTag(@Valid @RequestBody TagCreateRequestDto request) {
        Long userId = 1L; // TODO: 임시 사용자, 나중에 JWT/리졸버로 대체
        Tag tag = tagService.createTag(userId, request);
        return ResponseEntity
            .created(URI.create("/api/tags/" + tag.getId())) // ★ 201 + Location
            .body(TagResponseDto.from(tag));
    }

    // 태그 목록 조회 (현재 사용자 범위)
    @GetMapping
    public ResponseEntity<List<TagResponseDto>> listTags() {
        Long userId = 1L;
        Optional<Tag> tags = tagService.findTagsByUserId(userId);
        return ResponseEntity.ok(tags.stream().map(TagResponseDto::from).toList());
    }

    // 태그 전체 교체 성격의 업데이트(이름 바꾸기 정도면 PUT/PATCH 아무 쪽도 가능)
    @PutMapping("/{tagId}")
    public ResponseEntity<TagResponseDto> updateTag(@PathVariable Long tagId,
        @Valid @RequestBody TagUpdateRequestDto request) {
        Long userId = 1L;
        Tag tag = tagService.updateTag(userId, tagId, request);
        return ResponseEntity.ok(TagResponseDto.from(tag));
    }


    // 삭제
    @DeleteMapping("/{tagId}")
    public ResponseEntity<Void> deleteTag(@PathVariable Long tagId) {
        Long userId = 1L;
        tagService.deleteTag(userId, tagId);
        return ResponseEntity.noContent().build(); // ★ 204
    }
}
