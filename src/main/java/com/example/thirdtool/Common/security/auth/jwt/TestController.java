package com.example.thirdtool.Common.security.auth.jwt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class TestController {

    @GetMapping("/test")
    public Map<String, Object> test() {
        // JWT 필터를 통과하면 SecurityContext에 인증 정보가 담깁니다.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("JWT 필터 통과 후 Authentication 객체: {}", authentication);

        // 인증된 사용자 이름을 가져옵니다.
        String username = authentication.getName();

        // JWT에서 가져온 권한 정보를 가져옵니다.
        String role = authentication.getAuthorities().iterator().next().getAuthority();

        return Map.of(
                "message", "JWT 인증 성공!",
                "username", username,
                "role", role
                     );
    }
}