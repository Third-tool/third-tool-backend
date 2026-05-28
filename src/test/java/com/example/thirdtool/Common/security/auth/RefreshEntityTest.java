package com.example.thirdtool.Common.security.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RefreshEntity")
class RefreshEntityTest {

    @Test
    @DisplayName("ofNew는 id 없이 username/refresh 생성")
    void ofNew_returnsNewEntityWithoutId() {
        RefreshEntity entity = RefreshEntity.ofNew("alice", "rt-1");

        assertThat(entity.getId()).isNull();
        assertThat(entity.getUsername()).isEqualTo("alice");
        assertThat(entity.getRefresh()).isEqualTo("rt-1");
    }

    @Test
    @DisplayName("updateRefresh는 refresh 값만 갱신한다")
    void updateRefresh_changesRefreshOnly() {
        RefreshEntity entity = RefreshEntity.ofNew("alice", "rt-old");

        entity.updateRefresh("rt-new");

        assertThat(entity.getRefresh()).isEqualTo("rt-new");
        assertThat(entity.getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("updateRefresh에 null 전달 시 예외")
    void updateRefresh_null_throws() {
        RefreshEntity entity = RefreshEntity.ofNew("alice", "rt-old");

        assertThatThrownBy(() -> entity.updateRefresh(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("updateRefresh에 빈 문자열 전달 시 예외")
    void updateRefresh_blank_throws() {
        RefreshEntity entity = RefreshEntity.ofNew("alice", "rt-old");

        assertThatThrownBy(() -> entity.updateRefresh("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
