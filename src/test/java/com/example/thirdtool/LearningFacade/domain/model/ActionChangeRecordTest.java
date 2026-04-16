package com.example.thirdtool.LearningFacade.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ActionChangeRecord")
class ActionChangeRecordTest {

    // ───────── 1. changed() ─────────

    @Test
    @DisplayName("changed() 생성 시 isChanged는 true를 반환한다")
    void changed_isChanged_true() {
        //given & when
        ActionChangeRecord record = ActionChangeRecord.changed("설계하다", "검증하다");

        //then
        assertThat(record.isChanged()).isTrue();
    }

    @Test
    @DisplayName("changed() 생성 시 이전 동사와 새 동사가 결과 객체에 그대로 보존된다")
    void changed_값보존() {
        //given & when
        ActionChangeRecord record = ActionChangeRecord.changed("설계하다", "검증하다");

        //then
        assertThat(record.getPreviousDescription()).isEqualTo("설계하다");
        assertThat(record.getNewDescription()).isEqualTo("검증하다");
    }

    // ───────── 2. unchanged() ─────────

    @Test
    @DisplayName("unchanged() 생성 시 isChanged는 false를 반환한다")
    void unchanged_isChanged_false() {
        //given & when
        ActionChangeRecord record = ActionChangeRecord.unchanged("설계하다");

        //then
        assertThat(record.isChanged()).isFalse();
    }

    @Test
    @DisplayName("unchanged() 생성 시 이전 동사와 새 동사가 동일한 값으로 설정된다")
    void unchanged_전후값_동일() {
        //given & when
        ActionChangeRecord record = ActionChangeRecord.unchanged("설계하다");

        //then
        assertThat(record.getPreviousDescription()).isEqualTo(record.getNewDescription());
    }
}