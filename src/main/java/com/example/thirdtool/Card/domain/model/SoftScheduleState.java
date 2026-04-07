package com.example.thirdtool.Card.domain.model;

public enum SoftScheduleState {

    FRESH,
    INTERVAL_1D,
    INTERVAL_3D,
    INTERVAL_7D,
    INTERVAL_14D,
    INTERVAL_21D,
    NOT_YET;

    /** 노출 가능 여부를 반환한다. NOT_YET만 false를 반환한다. */
    public boolean isAvailable() {
        return this != NOT_YET;
    }
}