package com.example.thirdtool.Tag.presentation.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.thirdtool.Tag.application.service.TagService;
import com.example.thirdtool.Tag.domain.model.Tag;
import com.example.thirdtool.Tag.domain.repository.TagRepository;
import com.example.thirdtool.Tag.presentation.dto.TagCreateRequestDto;
import com.example.thirdtool.User.domain.model.User;
import com.example.thirdtool.User.domain.repository.UserRepository;
import com.example.thirdtool.common.config.NoJpaAuditingConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = TagController.class)
@Import(NoJpaAuditingConfig.class)
class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private TagService tagService;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @Test
    void createTag_success() throws Exception {
        // given
        TagCreateRequestDto requestDto = new TagCreateRequestDto("Java");
        User mockUser = new User(1L, "qwer@12324.com", "12345678"); // 실제 User 엔티티와 맞게 생성
        Tag mockTag = Tag.builder()
                .id(1L)
                    .displayName("Java")
                        .nameKey("java")
                            .user(mockUser)
                                .build();

        given(tagService.createTag(eq(1L), any(TagCreateRequestDto.class))).willReturn(mockTag);

        // when & then
        mockMvc.perform(post("/api/tags")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "name": "Java"
                        }
                        """))
            .andExpect(status().isCreated()) // 201
            .andExpect(header().string("Location", "/api/tags/1"))
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.name").value("Java"));

    }

    @Test
    void listTags() {
    }

    @Test
    void updateTag() {
    }

    @Test
    void deleteTag() {
    }
}