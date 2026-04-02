package com.example.thirdtool.Card.domain.model;

public enum SoftScheduleState {
    FRESH,
    INTERVAL_1D,
    INTERVAL_3D,
    INTERVAL_7D,
    NOT_YET;

    public boolean isAvailable() {
        return this != NOT_YET;
    }
}