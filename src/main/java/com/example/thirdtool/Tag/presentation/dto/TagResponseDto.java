package com.example.thirdtool.Tag.presentation.dto;

import com.example.thirdtool.Tag.domain.model.Tag;

public record TagResponseDto(Long id, String name) {
    public static TagResponseDto from(Tag tag) {
        return new TagResponseDto(tag.getId(), tag.getDisplayName());
    }
}
