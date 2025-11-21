package com.example.thirdtool.Common.config;


import com.example.thirdtool.Common.security.filter.JWTFilter;
import com.example.thirdtool.User.domain.model.CustomOAuth2User;
import com.example.thirdtool.User.domain.model.UserRoleType;
import com.example.thirdtool.Common.security.auth.jwt.JwtService;
import com.example.thirdtool.User.domain.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final String[] AUTH_ALLOWLIST = {
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/join",
            "/Join/**",
            "/login",
            "/jwt/**",
            "/token/**",
            "/login/**",
            "/images/**",
            "/kakao/**",
            "/app/token/**",
            "/api/auth/refresh",
            "/social/login/**",
            "/oauth2/**",
            "/actuator/health"// ✅ 카카오/네이버 로그인 엔드포인트도 화이트리스트에 추가
    };

    private final UserRepository userRepository;

    public SecurityConfig(
            UserRepository userRepository
                         ) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        log.info("[SecurityConfig] 초기화 완료 ✅");
        log.info("userRepository: {}", userRepository != null);
    }


    // 커스텀 자체 로그인 필터를 위한 AuthenticationManager Bean 수동 등록
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    // 권한 계층
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withRolePrefix("ROLE_")
                                .role(UserRoleType.ADMIN.name()).implies(UserRoleType.USER.name())
                                .build();
    }

    // 비밀번호 단방향(BCrypt) 암호화용 Bean
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:5173",
                "http://localhost:8080",
                "http://localhost:3000",
                "https://thirdstool.com" ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    // SecurityFilterChain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        // ==============================
        // 1️⃣ 기본 보안 설정
        // ==============================
        http
                .csrf(AbstractHttpConfigurer::disable)              // JWT 환경에서는 CSRF 불필요
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .formLogin(AbstractHttpConfigurer::disable)         // Form 로그인 비활성화
                .httpBasic(AbstractHttpConfigurer::disable);        // Basic 인증 비활성화

        // ==============================
        // 2️⃣ 세션 관리 (Stateless)
        // ==============================
        http
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        // ==============================
        // 4️⃣ 인가 (Authorization)
        // ==============================
        http
                .authorizeHttpRequests(auth -> auth
                                // ✅ 인증 없이 접근 가능한 화이트리스트
                                .requestMatchers(AUTH_ALLOWLIST).permitAll()
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/oauth2/**",
                                        "/social/**",
                                        "/health",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**",
                                        "/v3/api-docs.yaml",
                                        "/swagger-resources/**",
                                        "/webjars/**",
                                        "/custom-api-docs/**",     // ✅ Swagger 커스텀 문서 경로 추가
                                        "/swagger-config",          // ✅ swagger-ui가 자동으로 호출하는 경로
                                        "/swagger-ui/swagger-config"
                                                ).permitAll()

                                // ✅ 회원가입/중복확인 API
                                .requestMatchers(HttpMethod.POST, "/user", "/user/exist").permitAll()

                                // ✅ USER 전용 API
                                .requestMatchers(HttpMethod.GET, "/user").permitAll()
                                .requestMatchers(HttpMethod.PUT, "/user").hasRole(UserRoleType.USER.name())
                                .requestMatchers(HttpMethod.DELETE, "/user").hasRole(UserRoleType.USER.name())

                                // ✅ 나머지 요청은 인증 필요
                                .anyRequest().authenticated()
                                      );

        // ==============================
        // 5️⃣ 예외 처리 (401 / 403)
        // ==============================
        http
                .exceptionHandling(e -> e
                                .authenticationEntryPoint((req, res, ex) -> {
                                    res.sendError(HttpServletResponse.SC_UNAUTHORIZED);  // 401
                                })
                                .accessDeniedHandler((req, res, ex) -> {
                                    res.sendError(HttpServletResponse.SC_FORBIDDEN);     // 403
                                })
                                  );

        // ==============================
        // 6️⃣ JWT 인증 필터 추가
        // ==============================
        http.addFilterBefore(new JWTFilter(userRepository), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}