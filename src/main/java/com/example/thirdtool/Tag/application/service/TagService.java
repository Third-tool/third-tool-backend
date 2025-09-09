package com.example.thirdtool.Tag.application.service;

import com.example.thirdtool.Tag.domain.model.Tag;
import com.example.thirdtool.Tag.domain.repository.TagRepository;
import com.example.thirdtool.Tag.presentation.dto.TagCreateRequestDto;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;

@RequiredArgsConstructor
@Slf4j
@Service
public class TagService { //관리자용 태그 생성, 삭제, 수정

    private final TagRepository tagRepository;

    @Transactional
    public Tag createTag(TagCreateRequestDto tagCreateRequestDto) { //태그 생성
        Tag tag = Tag.of(tagCreateRequestDto.name());
        return tagRepository.save(tag);
    }

    @Transactional
    public void deleteTag(Long tagId) {  //태그 삭제
        Tag tag = tagRepository.findById(tagId)
            .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
        tagRepository.delete(tag);
    }

    @Transactional
    public Tag updateTag(Long tagId, TagCreateRequestDto tagCreateRequestDto) { //태그 수정
        Tag tag = tagRepository.findById(tagId)
            .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
        tag.updateName(tagCreateRequestDto.name());
        return tag;
    }


}