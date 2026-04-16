package com.example.thirdtool.LearningFacade.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ConceptChangeRecord")
class ConceptChangeRecordTest {

    // ───────── 1. changed() ─────────

    @Test
    @DisplayName("changed() 생성 시 isChanged는 true를 반환한다")
    void changed_isChanged_true() {
        //given & when
        ConceptChangeRecord record = ConceptChangeRecord.changed("백엔드", "분산 시스템 설계자");

        //then
        assertThat(record.isChanged()).isTrue();
    }

    @Test
    @DisplayName("changed() 생성 시 이전 컨셉과 새 컨셉이 결과 객체에 그대로 보존된다")
    void changed_값보존() {
        //given & when
        ConceptChangeRecord record = ConceptChangeRecord.changed("백엔드", "분산 시스템 설계자");

        //then
        assertThat(record.getPreviousConcept()).isEqualTo("백엔드");
        assertThat(record.getNewConcept()).isEqualTo("분산 시스템 설계자");
    }

    @Test
    @DisplayName("v1에서 isDrifted()는 isChanged()와 동일한 값을 반환한다")
    void changed_isDrifted_isChanged와동일_v1() {
        //given & when
        ConceptChangeRecord record = ConceptChangeRecord.changed("백엔드", "분산 시스템 설계자");

        //then
        assertThat(record.isDrifted()).isEqualTo(record.isChanged());
    }

    // ───────── 2. unchanged() ─────────

    @Test
    @DisplayName("unchanged() 생성 시 isChanged는 false를 반환한다")
    void unchanged_isChanged_false() {
        //given & when
        ConceptChangeRecord record = ConceptChangeRecord.unchanged("백엔드");

        //then
        assertThat(record.isChanged()).isFalse();
    }

    @Test
    @DisplayName("unchanged() 생성 시 isDrifted도 false를 반환한다")
    void unchanged_isDrifted_false() {
        //given & when
        ConceptChangeRecord record = ConceptChangeRecord.unchanged("백엔드");

        //then
        assertThat(record.isDrifted()).isFalse();
    }
}