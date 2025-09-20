package com.example.thirdtool.common.config;

import java.util.Optional;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.AuditorAware;

@TestConfiguration
public class NoJpaAuditingConfig {
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.empty(); // 더미 빈
    }
}
