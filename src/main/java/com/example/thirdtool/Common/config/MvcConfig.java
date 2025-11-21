package com.example.thirdtool.Common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry corsRegistry) {

        corsRegistry.addMapping("/**")
                    .allowedOrigins(
                            "http://localhost:5173",
                            "http://localhost:8080",
                            "http://localhost:3000",
                            "https://thirdstool.com",       // ✅ FE 배포 도메인
                            "https://www.thirdstool.com" // www 쓰면 추가
                                   )
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowCredentials(true)
                    .allowedHeaders("*")
                    .exposedHeaders("Set-Cookie", "Authorization");
    }
}