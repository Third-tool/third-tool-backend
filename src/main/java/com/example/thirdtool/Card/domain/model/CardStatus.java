package com.example.thirdtool.Card.domain.model;

public enum CardStatus {

    /**
     * 현재 전면 노출 구간.
     * 지금 모르거나 익숙하지 않아 집중 반복이 필요한 카드를 올려두는 스테이징 공간.
     * Card 생성 시 기본값이다.
     */
    ON_FIELD,

    /**
     * 전면 루프에서 내려간 후방 대기 구간.
     * 배경 지식으로 보관되며 언제든 ON_FIELD로 재호출 가능하다.
     */
    ARCHIVE
}
