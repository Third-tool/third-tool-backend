package com.example.thirdtool.Card.domain.model;

public enum MainContentType {
    TEXT_ONLY,
    IMAGE_ONLY,
    MIXED;

    /**
     * textContent, imageUrl 존재 여부에 따라 ContentType을 결정한다.
     * 이 메서드는 MainNote 생성 로직 안에서만 호출되어야 한다.
     */
    static MainContentType resolve(boolean hasText, boolean hasImage) {
        if (hasText && hasImage) return MIXED;
        if (hasText) return TEXT_ONLY;
        if (hasImage) return IMAGE_ONLY;
        throw new IllegalStateException("텍스트와 이미지가 모두 없는 상태에서 ContentType을 결정할 수 없습니다.");
    }
}