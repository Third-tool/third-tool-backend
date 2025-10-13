package com.example.thirdtool.User.infrastructure.kakao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KakaoMemberRepository extends JpaRepository<KakaoMember, Long> {
    Optional<KakaoMember> findBySocialId(String socialId);
}