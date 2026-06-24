package com.example.thirdtool.Common.observability;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Story 4-1: Actuator Prometheus endpoint 노출·민감 endpoint 차단·메트릭 라벨·histogram bucket
 * 회귀 안전망. SecurityConfig·ActuatorMetricsConfig·application-test.yml의 management 설정이
 * 한 단위로 동작하는지 단일 SpringBootTest 컨텍스트에서 검증.
 */
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Actuator Prometheus endpoint 통합")
class ActuatorMetricsIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("/actuator/prometheus는 200 OK + application=thirdtool 공통 태그 + http_server_requests 시리즈 노출")
    void prometheus_endpoint_노출_application_태그_http_server_requests_시리즈() {
        // 메트릭 누적을 위해 임의 endpoint 호출 1회 (정상/오류 무관)
        restTemplate.getForEntity("/health", String.class);

        ResponseEntity<String> response = scrapePrometheus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        assertThat(body).isNotBlank();
        assertThat(body)
                .as("공통 태그 application=thirdtool")
                .contains("application=\"thirdtool\"");
        assertThat(body)
                .as("http_server_requests 시리즈 (count·sum) 노출")
                .contains("http_server_requests_seconds_count");
    }

    @Test
    @DisplayName("/actuator/prometheus 응답에 http_server_requests_seconds_bucket(histogram)이 포함된다")
    void prometheus_응답에_histogram_bucket이_포함된다() {
        restTemplate.getForEntity("/health", String.class);
        restTemplate.getForEntity("/actuator/health", String.class);

        ResponseEntity<String> response = scrapePrometheus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .as("percentilesHistogram=true → histogram_quantile 계산 가능한 bucket 시리즈")
                .contains("http_server_requests_seconds_bucket");
    }

    /**
     * Prometheus scrape endpoint는 {@code text/plain; version=0.0.4} 또는 OpenMetrics 형식만 응답.
     * TestRestTemplate 기본 Accept 헤더가 {@code text/plain, application/json, ...}이지만 OpenMetrics
     * 협상 단계에서 NPE를 유발할 수 있어 실 운영의 Prometheus scraper 행태처럼 Accept를 명시한다.
     */
    private ResponseEntity<String> scrapePrometheus() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.parseMediaType("text/plain"), MediaType.ALL));
        return restTemplate.exchange(
                "/actuator/prometheus", HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    @Test
    @DisplayName("/actuator/health는 익명 호출 200 OK + UP")
    void actuator_health는_익명_호출_200_OK_UP() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("/actuator/info는 익명 호출 200 OK (화이트리스트 허용)")
    void actuator_info는_익명_호출_200_OK() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/info", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("/actuator/env는 노출 제외 + denyAll로 차단되어 본문이 반환되지 않는다")
    void actuator_env는_노출_제외_및_denyAll로_차단된다() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/env", String.class);

        assertThat(response.getStatusCode())
                .as("차단 status — 401(인증 요구) 또는 403(권한 거부) 또는 404(endpoint 미노출)")
                .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
        assertThat(response.getBody() == null || !response.getBody().contains("DB_PASSWORD"))
                .as("환경변수 본문이 응답에 노출되어선 안 된다")
                .isTrue();
    }

    @Test
    @DisplayName("/actuator/heapdump도 노출 제외 + denyAll로 차단되어 메모리 덤프가 응답되지 않는다")
    void actuator_heapdump는_노출_제외_및_denyAll로_차단된다() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/heapdump", String.class);

        assertThat(response.getStatusCode())
                .isIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("/actuator/** 경로의 http_server_requests 시리즈는 메트릭에 누적되지 않는다 (카디널리티 차단)")
    void actuator_경로의_http_server_requests_메트릭은_시리즈에_포함되지_않는다() {
        // 10초 scrape 모사 — 여러 번 호출해 자체 누적 발생 가능 상황을 만든다
        for (int i = 0; i < 3; i++) {
            scrapePrometheus();
            restTemplate.getForEntity("/actuator/health", String.class);
        }
        restTemplate.getForEntity("/health", String.class);

        ResponseEntity<String> response = scrapePrometheus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = response.getBody();
        // http_server_requests* (서버 inbound) 시리즈 한 줄 안에 uri="/actuator/..." 라벨이 동시 존재하지 않아야 한다.
        // (http_client_requests는 TestRestTemplate outbound 시리즈로 별개. 본 PR의 deny 대상 아님)
        assertThat(body)
                .as("http_server_requests*{uri=\"/actuator/...\"} 시리즈가 deny되어 출력되지 않아야 한다")
                .doesNotContainPattern("http_server_requests[a-z_]*\\{[^}]*uri=\"/actuator/[^\"]+\"");
    }
}
