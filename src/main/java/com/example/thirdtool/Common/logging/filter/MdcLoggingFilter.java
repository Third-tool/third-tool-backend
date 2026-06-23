package com.example.thirdtool.Common.logging.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 모든 HTTP 요청에 requestId를 부여하고 MDC·응답 헤더로 전파하는 필터 (Story 2-1).
 *
 * <p>SecurityFilterChain 가장 앞단에서 동작하며 다음을 보장한다.
 * <ul>
 *   <li>X-Request-Id 헤더가 정상이면 그대로 사용, 없거나 blank/{@value #MAX_LEN}자 초과면 서버가 UUID 생성</li>
 *   <li>MDC 키 4종(requestId, traceId(=requestId), method, path)을 logback-spring.xml 화이트리스트와 정합하게 주입</li>
 *   <li>응답 헤더 X-Request-Id echo back — 클라이언트가 동일 ID로 문의·추적 가능</li>
 *   <li>request.start / request.end 로그 INFO + status + durationMs</li>
 *   <li>예외 경로 포함 finally 블록에서 {@link MDC#clear()} — 스레드 풀 재사용 시 누수 차단</li>
 * </ul>
 *
 * <p>userId 주입은 Story 2-2 별도 처리. traceId == requestId는 v1 단순화 — 분산 트레이싱
 * 도입 시 분리. 자세한 결정 배경은 ADR008.
 */
@Slf4j
public class MdcLoggingFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Request-Id";
    static final String MDC_REQUEST_ID = "requestId";
    static final String MDC_TRACE_ID = "traceId";
    static final String MDC_METHOD = "method";
    static final String MDC_PATH = "path";
    static final int MAX_LEN = 64;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = resolveRequestId(request.getHeader(HEADER));
        long startedAt = System.currentTimeMillis();
        try {
            MDC.put(MDC_REQUEST_ID, requestId);
            MDC.put(MDC_TRACE_ID, requestId);
            MDC.put(MDC_METHOD, request.getMethod());
            MDC.put(MDC_PATH, request.getRequestURI());
            response.setHeader(HEADER, requestId);

            log.info("request.start clientIp={}", clientIp(request));

            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            log.info("request.end status={} durationMs={}", response.getStatus(), durationMs);
            MDC.clear();
        }
    }

    private String resolveRequestId(String incoming) {
        if (incoming == null || incoming.isBlank() || incoming.length() > MAX_LEN) {
            return UUID.randomUUID().toString();
        }
        return incoming;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}
