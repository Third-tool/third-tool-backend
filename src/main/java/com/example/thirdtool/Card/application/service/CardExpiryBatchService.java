package com.example.thirdtool.Card.application.service;

import com.example.thirdtool.Card.domain.model.ArchiveReason;
import com.example.thirdtool.Card.domain.model.Card;
import com.example.thirdtool.Card.domain.model.CardExpiryPolicy;
import com.example.thirdtool.Card.domain.model.CardStatus;
import com.example.thirdtool.Card.domain.model.CardStatusHistoryAppender;
import com.example.thirdtool.Card.domain.model.OnFieldBudget;
import com.example.thirdtool.Card.infrastructure.persistence.CardRepository;
import com.example.thirdtool.Deck.domain.model.Deck;
import com.example.thirdtool.UserSchedule.application.service.UserScheduleQueryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * product-card.md Epic 2 Story 2-2 — 야간 배치로 maxDuration / maxView 예산 초과 카드를 자동 ARCHIVE 한다.
 *
 * <p>실행 주기는 `card.expiry.cron`(기본 매일 03:00). 비활성화는 `-` 값으로 가능.
 * (Spring `@Scheduled`는 cron="-"을 disabled 트리거로 해석).
 *
 * <p>처리 흐름
 * <ol>
 *   <li>ON_FIELD 상태(deleted=false) 후보 카드 일괄 조회.</li>
 *   <li>사용자별로 묶어 각 사용자당 {@link UserScheduleQueryService#currentMode} 1회만 조회.</li>
 *   <li>각 카드에 대해 {@link CardExpiryPolicy#expire}로 만료 사유 판정 → ARCHIVE 전환.</li>
 *   <li>만료된 카드는 history append + 소속 Deck 재계산을 모음 단위로 1회 실행.</li>
 * </ol>
 *
 * <p>멱등성 — 같은 카드가 두 번 archive되어도 도메인 행위가 no-op이므로 안전.
 * 재실행 / 동시 실행 모두 동일 결과.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CardExpiryBatchService {

    private static final Logger log = LoggerFactory.getLogger(CardExpiryBatchService.class);

    private final CardRepository cardRepository;
    private final CardStatusHistoryAppender historyAppender;
    private final CardExpiryPolicy cardExpiryPolicy;
    private final UserScheduleQueryService userScheduleQueryService;

    /**
     * Spring Scheduler 진입점. 결과만 info 로그로 기록한다.
     * 단위 테스트는 {@link #processExpired()} 를 직접 호출한다.
     */
    @Scheduled(cron = "${card.expiry.cron:0 0 3 * * *}")
    public void runScheduledExpiry() {
        log.info("[CardExpiryBatch] scheduled run started");
        ExpiryResult result = processExpired();
        log.info(
                "[CardExpiryBatch] done — candidates={}, archived={}, reasons[MAX_VIEW={}, MAX_DURATION={}]",
                result.candidates(), result.archived(),
                result.maxViewCount(), result.maxDurationCount()
        );
    }

    public ExpiryResult processExpired() {
        List<Card> candidates = cardRepository.findAllByStatus(CardStatus.ON_FIELD);
        if (candidates.isEmpty()) {
            return ExpiryResult.empty();
        }

        Map<Long, List<Card>> byUser = candidates.stream()
                .collect(Collectors.groupingBy(c -> c.getDeck().getUser().getId()));

        int archived = 0;
        int maxViewCount = 0;
        int maxDurationCount = 0;
        Set<Deck> changedDecks = new HashSet<>();

        for (Map.Entry<Long, List<Card>> entry : byUser.entrySet()) {
            Long userId = entry.getKey();
            // Story-CARD-E1-S1-4 — resolveOnFieldBudget() 폐기 → currentMode() 우회. PR#2에서 함께 제거.
            OnFieldBudget budget = userScheduleQueryService.currentMode(userId).toOnFieldBudget();
            for (Card card : entry.getValue()) {
                CardStatus before = card.getStatus();
                Optional<ArchiveReason> reason = cardExpiryPolicy.expire(card, budget);
                if (reason.isEmpty()) continue;

                historyAppender.append(card, before, card.getStatus(), reason.get());
                changedDecks.add(card.getDeck());
                archived++;
                if (reason.get() == ArchiveReason.MAX_VIEW) {
                    maxViewCount++;
                } else if (reason.get() == ArchiveReason.MAX_DURATION) {
                    maxDurationCount++;
                }
            }
        }

        // 한 Deck에 여러 카드가 archive되어도 recalculate는 1회만 호출되도록 모은다.
        changedDecks.forEach(Deck::recalculateProgressStatus);

        return new ExpiryResult(candidates.size(), archived, maxViewCount, maxDurationCount);
    }

    public record ExpiryResult(int candidates, int archived, int maxViewCount, int maxDurationCount) {
        static ExpiryResult empty() {
            return new ExpiryResult(0, 0, 0, 0);
        }
    }
}
