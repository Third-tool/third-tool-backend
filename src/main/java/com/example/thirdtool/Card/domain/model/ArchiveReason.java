package com.example.thirdtool.Card.domain.model;

public enum ArchiveReason {

    /**
     * 유저가 직접 Archive로 이동한 경우.
     * 수동 Archive API 호출 시 기록된다.
     */
    MANUAL,

    /**
     * viewCount가 maxView에 도달한 경우.
     * 리뷰 세션 중 노출 횟수 예산 소진 시 즉시 전환된다.
     */
    MAX_VIEW,

    /**
     * enteredFieldAt 기준 체류 기간이 maxDuration을 초과한 경우.
     * 배치 감지 시 전환된다.
     */
    MAX_DURATION
}