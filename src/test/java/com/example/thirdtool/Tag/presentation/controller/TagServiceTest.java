package com.example.thirdtool.Tag.presentation.controller;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.thirdtool.Common.NameNormalizer;
import com.example.thirdtool.Tag.application.service.TagService;
import com.example.thirdtool.Tag.domain.model.Tag;
import com.example.thirdtool.Tag.domain.repository.TagRepository;
import com.example.thirdtool.Tag.presentation.dto.TagCreateRequestDto;
import com.example.thirdtool.Tag.presentation.dto.TagUpdateRequestDto;
import com.example.thirdtool.User.domain.model.User;
import com.example.thirdtool.User.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Transactional
class TagServiceTest {

    @Autowired
    private TagService tagService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("태그를 성공적으로 생성해야 한다")
    void createTag_Success() {
        // given
        User testUser = userRepository.save(User.of("testuser@naver.com", "12345678"));
        TagCreateRequestDto requestDto = new TagCreateRequestDto("Java");

        // when
        Tag createdTag = tagService.createTag(testUser.getId(), requestDto);

        // then (결과 검증)
        Long newTagId = createdTag.getId();
        assertThat(newTagId).isNotNull();

        // ✅ DB에서 방금 저장된 Tag를 다시 조회합니다.
        Tag foundTag = tagRepository.findById(newTagId)
            .orElseThrow(() -> new AssertionError("저장된 태그를 찾을 수 없습니다."));

        // ✅ 다시 조회한 객체로 연관관계를 검증합니다.
        assertThat(foundTag.getUser()).isNotNull();
        assertThat(foundTag.getUser().getId()).isEqualTo(testUser.getId());
        assertThat(foundTag.getNameKey()).isEqualTo("java");
    }

    @Test
    void deleteTag() {
        User user1 = userRepository.save(User.of("qwer@1234.com", "12345678"));
        Tag tag = Tag.builder()
            .displayName("Java")
            .nameKey(NameNormalizer.toKey("java"))
            .user(user1)
            .build();
        tagRepository.save(tag);
        tagService.deleteTag(user1.getId(), tag.getId());
        assertEquals(0, tagRepository.findAll().size());
    }

    @Test
    void updateTag() {
        User user = userRepository.save(User.of("qwer@1234.com", "12345678"));
        TagUpdateRequestDto requestDto = new TagUpdateRequestDto("C");
        Tag tag = Tag.builder()
            .displayName("Java")
            .nameKey(NameNormalizer.toKey("java"))
            .user(userRepository.findById(1L).orElseThrow())
            .build();

        tagRepository.save(tag);
        Tag updatedTag = tagService.updateTag(user.getId(), tag.getId(), requestDto);

        em.flush();
        em.clear();

        assertThat(updatedTag.getDisplayName()).isEqualTo("C");
        assertThat(updatedTag.getNameKey()).isEqualTo("c");
    }

    @Test
    void findTagsByUserId() {
        User user = userRepository.save(User.of("qwer@1234.com", "12345678"));
        Tag tag = Tag.builder()
            .displayName("Java")
            .nameKey(NameNormalizer.toKey("java"))
            .user(userRepository.findById(1L).orElseThrow())
            .build();
        tagRepository.save(tag);
        Tag tag1 = Tag.builder()
            .displayName("C")
            .nameKey(NameNormalizer.toKey("c"))
            .user(userRepository.findById(1L).orElseThrow())
            .build();
        tagRepository.save(tag1);
        assertEquals(2, tagRepository.findAll().size());
    }

    /*  @Transactional(readOnly = true)
    public Optional<Tag> findTagsByUserId(Long userId){  //모든 태그 조회
        return tagRepository.findByUserId(userId);
    }*/
}