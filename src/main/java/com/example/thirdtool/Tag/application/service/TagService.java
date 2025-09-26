package com.example.thirdtool.Tag.application.service;

import static com.example.thirdtool.Common.NameNormalizer.toKey;


import com.example.thirdtool.Common.NameNormalizer;
import com.example.thirdtool.Tag.domain.model.Tag;
import com.example.thirdtool.Tag.domain.repository.TagRepository;
import com.example.thirdtool.Tag.presentation.dto.TagCreateRequestDto;

import com.example.thirdtool.Tag.presentation.dto.TagUpdateRequestDto;
import com.example.thirdtool.User.domain.model.User;
import com.example.thirdtool.User.domain.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
@Service
public class TagService { //유저 개인이 태그 관리

    private final TagRepository tagRepository;
    private final UserRepository userRepository;

    @Transactional
    public Tag createTag(Long userId, TagCreateRequestDto tagCreateRequestDto) {
        String key = toKey(tagCreateRequestDto.name());
        if (key.isBlank()) throw new IllegalArgumentException("태그 이름이 비어있습니다.");
        User user = userRepository.getReferenceById(userId);//태그 생성
        tagRepository.findByNameKeyIgnoreCaseAndUserId(tagCreateRequestDto.name(), userId)
            .ifPresent(tag -> {
                throw new IllegalArgumentException("이미 존재하는 태그입니다");
            });
        Tag tag = Tag.of(NameNormalizer.toDisplay(tagCreateRequestDto.name()),user);
        return tagRepository.save(tag);
    }

    @Transactional
    public void deleteTag(Long userId, Long tagId) {  //태그 삭제
        Tag tag = tagRepository.findById(tagId)
            .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 태그입니다"));
        tagRepository.delete(tag);
    }

    @Transactional
    public Tag updateTag(Long userId, Long tagId, TagUpdateRequestDto req) { //태그 수정
        Tag tag = tagRepository.findByIdAndUserId(userId,tagId)
            .orElseThrow(() -> new IllegalArgumentException("Tag not found"));
        String key = NameNormalizer.toKey(req.name());
        if (key.isBlank()) {
            throw new IllegalArgumentException("태그 이름이 비어 있습니다.");
        }

        if (tagRepository.findByNameKeyIgnoreCaseAndUserId(key,userId).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 태그입니다.");
        }

        tag.updateName(NameNormalizer.toDisplay(req.name()));
        return tag;
    }

    @Transactional
    public Optional<Tag> findTagsByUserId(Long userId){  //모든 태그 조회
        return tagRepository.findByUserId(userId);
    }
}