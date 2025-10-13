package com.example.thirdtool.User.infrastructure.Naver;


import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NaverMemberRepository extends JpaRepository<NaverMember, Long> {
    Optional<NaverMember> findBySocialId(String naverId);
}
