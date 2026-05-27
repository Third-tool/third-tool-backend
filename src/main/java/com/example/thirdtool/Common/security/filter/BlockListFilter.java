package com.example.thirdtool.Common.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 악성 URL 패턴 차단 필터 (Story 3-3).
 *
 * JWTFilter에서 분리하여 책임을 명확히 한다. JWTFilter는 JWT 인증만 담당.
 *
 * 차단 패턴은 코드로 고정 (추후 application.yml 외부화 검토).
 */
@Slf4j
public class BlockListFilter extends OncePerRequestFilter {

    private static final List<String> SUFFIX_PATTERNS = List.of(".php", ".aspx");
    private static final List<String> CONTAINS_PATTERNS = List.of("/wp-", "/cgi-bin/");

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        boolean blocked = SUFFIX_PATTERNS.stream().anyMatch(uri::endsWith)
                || CONTAINS_PATTERNS.stream().anyMatch(uri::contains);

        if (blocked) {
            log.debug("[BlockList] 차단 패턴 URI: {}", uri);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
