package com.example.thirdtool.Common.logging.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * MdcLoggingFilter의 동시성·MDC 누수 회귀 테스트.
 * AC: "동시 요청 requestId 충돌 없음" + "스레드 풀 재사용 시 이전 MDC 누수 없음".
 */
@DisplayName("MdcLoggingFilter 동시성")
class MdcLoggingFilterConcurrencyTest {

    private final MdcLoggingFilter filter = new MdcLoggingFilter();

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    @DisplayName("두 스레드에서 동시 요청해도 각자 고유한 requestId를 받는다")
    void 두_스레드에서_동시_요청해도_각자_고유한_requestId를_받는다() throws Exception {
        CountDownLatch bothInsideChain = new CountDownLatch(2);
        CountDownLatch releaseChain = new CountDownLatch(1);

        Callable<String> task = () -> {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/concurrent");
            MockHttpServletResponse res = new MockHttpServletResponse();
            AtomicReference<String> seen = new AtomicReference<>();
            FilterChain chain = mock(FilterChain.class);
            doAnswer(inv -> {
                seen.set(MDC.get(MdcLoggingFilter.MDC_REQUEST_ID));
                bothInsideChain.countDown();
                releaseChain.await(10, TimeUnit.SECONDS);
                return null;
            }).when(chain).doFilter(any(), any());

            filter.doFilter(req, res, chain);
            return seen.get();
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> a = pool.submit(task);
            Future<String> b = pool.submit(task);

            assertThat(bothInsideChain.await(10, TimeUnit.SECONDS))
                    .as("두 스레드가 동시에 chain 내부에 진입").isTrue();
            releaseChain.countDown();

            String idA = a.get(10, TimeUnit.SECONDS);
            String idB = b.get(10, TimeUnit.SECONDS);

            assertThat(idA).isNotBlank();
            assertThat(idB).isNotBlank();
            assertThat(idA).isNotEqualTo(idB);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("100회 연속 요청해도 모든 requestId가 유일하다 (서버 생성 UUID 충돌 회귀)")
    void 백회_연속_요청해도_모든_requestId가_유일하다() throws Exception {
        Set<String> collected = Collections.synchronizedSet(new HashSet<>());

        for (int i = 0; i < 100; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("GET", "/loop/" + i);
            MockHttpServletResponse res = new MockHttpServletResponse();
            FilterChain chain = mock(FilterChain.class);
            doAnswer(inv -> {
                collected.add(MDC.get(MdcLoggingFilter.MDC_REQUEST_ID));
                return null;
            }).when(chain).doFilter(any(), any());

            filter.doFilter(req, res, chain);
        }

        assertThat(collected).hasSize(100);
    }

    @Test
    @DisplayName("동일 스레드에서 연속 요청 시 이전 요청의 MDC가 다음 요청에 누수되지 않는다")
    void 동일_스레드에서_연속_요청_시_이전_요청의_MDC가_다음_요청에_누수되지_않는다() throws Exception {
        // 1차 요청: chain 내부에서 화이트리스트 외 임의 키까지 추가하고 정상 종료
        MockHttpServletRequest first = new MockHttpServletRequest("GET", "/first");
        first.addHeader(MdcLoggingFilter.HEADER, "first-request");
        FilterChain firstChain = mock(FilterChain.class);
        doAnswer(inv -> {
            MDC.put("legacyKey", "leakCandidate");
            return null;
        }).when(firstChain).doFilter(any(), any());
        filter.doFilter(first, new MockHttpServletResponse(), firstChain);

        // 2차 요청: chain 진입 시점의 MDC 스냅샷을 캡처
        AtomicReference<Map<String, String>> secondSnapshot = new AtomicReference<>();
        MockHttpServletRequest second = new MockHttpServletRequest("GET", "/second");
        second.addHeader(MdcLoggingFilter.HEADER, "second-request");
        FilterChain secondChain = mock(FilterChain.class);
        doAnswer(inv -> {
            secondSnapshot.set(MDC.getCopyOfContextMap());
            return null;
        }).when(secondChain).doFilter(any(), any());
        filter.doFilter(second, new MockHttpServletResponse(), secondChain);

        Map<String, String> captured = secondSnapshot.get();
        assertThat(captured).isNotNull();
        assertThat(captured.get(MdcLoggingFilter.MDC_REQUEST_ID)).isEqualTo("second-request");
        assertThat(captured.get(MdcLoggingFilter.MDC_PATH)).isEqualTo("/second");
        assertThat(captured).doesNotContainKey("legacyKey");
    }
}
