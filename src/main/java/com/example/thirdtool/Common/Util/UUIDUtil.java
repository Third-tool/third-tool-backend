package com.example.thirdtool.Common.Util;

import de.huxhorn.sulky.ulid.ULID;

import java.util.UUID;


public class UUIDUtil {

    private static final ULID ulid = new ULID();

    /**
     * ✅ UUIDv4 (랜덤 기반)
     */
    public static String generateV4() {
        return UUID.randomUUID().toString();
    }

    /**
     * ✅ v7 대체용 (ULID 기반)
     *  - 시간순 정렬 가능
     *  - DB/ES 인덱싱 효율 ↑
     *  - Java 22에서 UUID.v7()으로 자연스럽게 대체 가능
     */
    public static String generateV7() {
        return ulid.nextULID();
    }

    /**
     * ✅ prefix 추가 버전 (카테고리 식별용)
     */
    public static String generateWithPrefix(String prefix) {
        return prefix + "_" + generateV7();
    }
}