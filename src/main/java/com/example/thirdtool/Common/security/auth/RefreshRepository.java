package com.example.thirdtool.Common.security.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshRepository extends JpaRepository<RefreshEntity, Long> {
    //리프레시 토큰 있는지 확인 용
    Boolean existsByRefresh(String refreshToken);

    @Transactional
    void deleteByRefresh(String refresh);

    @Transactional
    void deleteByUsername(String username);

    // 특정일 지난 refresh 토큰 삭제
    @Transactional
    void deleteByCreatedDateBefore(LocalDateTime createdDate);

    boolean existsByUsername(String username);

    //username 기반으로 찾을 때 바로 리프레쉬 토큰 반납하게 한다.
    String findByUsername(String username);

    // 새로 추가 (엔티티 자체 반환)
    Optional<RefreshEntity> findEntityByUsername(String username);
}
