package com.example.thirdtool.LearningFacade.domain.model;

import com.example.thirdtool.Common.Exception.ErrorCode.ErrorCode;
import com.example.thirdtool.LearningFacade.domain.exception.LearningFacadeDomainException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
        name = "learning_axis",
        // (facade_id, name, deleted_at) 3-column composite unique — Fix Axis↔Deck 완전 통합, 2026-07-01.
        // MySQL은 NULL 조합을 unique 검사에서 서로 다른 값으로 취급하므로:
        //   - 활성 축(deleted_at IS NULL)끼리는 (facade_id, name) 유일 보장
        //   - 소프트 삭제된 축(deleted_at != NULL)의 name을 재사용해도 삽입 성공
        // V14 마이그레이션과 동기화 유지.
        uniqueConstraints = @UniqueConstraint(
                name = "uk_learning_axis_facade_name",
                columnNames = {"learning_facade_id", "name", "deleted_at"}
        )
)
@SQLRestriction("deleted_at IS NULL")
public class LearningAxis {

    private static final int RECOMMENDED_TOPIC_LIMIT = 10;

    /**
     * Story 2-3 — 상위 N개 주제에 "지금 집중 중" 뱃지를 표시할 때 사용하는 임계값.
     * {@link AxisTopic#isFocused(int)}가 displayOrder ≤ 이 값일 때 true를 반환한다.
     * 주제 3개 미만이면 모든 주제가 자동으로 focused (displayOrder 1, 2, 3 모두 임계값 이하).
     */
    public static final int FOCUS_TOP_N = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "learning_axis_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_facade_id", nullable = false, updatable = false)
    private LearningFacade facade;

    // ─── Layer FK (Story-LT-E2-S2) ────────────────────────
    //
    // Layer 도입에 따른 상위 그룹핑 참조. 3-phase 마이그레이션 (V19)의 안전한 전이 기간 동안
    // nullable로 유지되며 Story-LT-E2-S3 백필 완료 후 이관 릴리스에서 NOT NULL로 전환된다.
    //
    // <p>도메인 진입점: {@link LearningLayer#addAxis} (blessed) 또는
    // {@link LearningFacade#addAxis(String)} (legacy — 내부적으로 default layer 라우팅)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "learning_layer_id", nullable = true, updatable = true)
    private LearningLayer layer;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @OneToMany(
            mappedBy      = "axis",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    @OrderBy("displayOrder ASC")
    private final List<AxisTopic> topics = new ArrayList<>();

    /**
     * Roadmap 챕터 노드 (Story-LT-E3-S3-6, 이슈 #15).
     * 각 노드가 챕터 하나 (title + rationale + body ASCII 통짜).
     * {@code orphanRemoval=false} — 노드 자체가 {@code @SQLDelete}로 소프트 삭제되므로 hard delete 발생하지 않는다.
     */
    @OneToMany(
            mappedBy      = "axis",
            cascade       = CascadeType.ALL,
            orphanRemoval = false,
            fetch         = FetchType.LAZY
    )
    @OrderBy("displayOrder ASC")
    private final List<AxisRoadmapNode> roadmapNodes = new ArrayList<>();

    /**
     * Selection 컨테이너 (Story-LT-E3-S3-9, 이슈 #16 · 이슈 #11 정책 계승).
     * 컨테이너 정책: name UNIQUE per axis, created_at DESC 정렬, hard delete.
     * {@code orphanRemoval=true} — 컨테이너 컬렉션에서 제거 시 hard delete.
     */
    @OneToMany(
            mappedBy      = "axis",
            cascade       = CascadeType.ALL,
            orphanRemoval = true,
            fetch         = FetchType.LAZY
    )
    @OrderBy("createdAt DESC")
    private final List<AxisSelection> selections = new ArrayList<>();

    // ─── Deck 폐기 흡수 필드 (LT E5 · M5 · Story 5-1) ─────────
    //
    // Deck BC 완전 폐기 (M5)로 인해 8 책임 필드 중 6개를 LearningAxis로 이전.
    // 계층 필드 (parentDeck·depth·subDecks)는 SDD 우선 정책으로 완전 폐기 (Layer가 유일 상위 개념).
    // scoring_algorithm_type (V1 legacy)는 이관 없이 _archived_deck에만 잔존.

    /**
     * 축 진행 상태 (Deck.progressStatus 이관).
     * 축에 속한 카드 상태 집계로 파생. Application Service가 카드 카운트 계산 후
     * {@link #recalculateProgressStatus(int, int)} 호출.
     * Epic 6 Story 6-4에서 Layer.progressStatus 파생의 소스.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "progress_status", nullable = false, length = 20)
    private AxisProgressStatus progressStatus = AxisProgressStatus.NOT_STARTED;

    /**
     * 학습 모드 (Deck.mode 이관 · SDD 우선 재정의).
     * 기존 {@code DeckMode.ON_FIELD/ARCHIVE} → 신규 {@link AxisLearningMode#STUDY}/{@link AxisLearningMode#REVIEW}.
     * V31 백필 정책: ON_FIELD→STUDY, ARCHIVE→REVIEW.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 20)
    private AxisLearningMode mode = AxisLearningMode.STUDY;

    /** 최근 접근 시각 (Deck.lastAccessed 이관). Review 세션 진입 시 갱신. */
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    /**
     * 대표 학습 자료 참조 (Deck.learningMaterialId 이관 · Long id).
     * BC 간 직접 객체 참조 회피 (docs/PACKAGE.md §6). 자료 삭제 시 null로 전환.
     */
    @Column(name = "learning_material_id")
    private Long learningMaterialId;

    /** 라이브러리 공개 여부 (Deck.onLibrary 이관). */
    @Column(name = "on_library", nullable = false)
    private boolean onLibrary = false;

    /** 공개 시각 (Deck.publishedAt 이관 · 라이브러리 정렬용). */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // ─── 표준 audit / soft delete ─────────────────────────

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Soft Delete 시각. null이면 활성 상태.
     * 클래스 상단 {@code @SQLRestriction("deleted_at IS NULL")}로 모든 read에서 자동 필터된다.
     * (Fix — Axis↔Deck 완전 통합, 2026-07-01)
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    private LearningAxis(LearningFacade facade, LearningLayer layer, String name, int displayOrder) {
        this.facade       = facade;
        this.layer        = layer;
        this.name         = name;
        this.displayOrder = displayOrder;
    }

    /**
     * Legacy 팩토리 (Layer 없이 생성) — {@link LearningFacade#addAxis(String)} 경로용.
     * <p>Layer는 {@code null}로 두고, LearningFacade가 default Uncategorized layer를 확보한 뒤
     * {@link #assignLayer(LearningLayer)}로 주입한다. 3-phase 마이그레이션 종료 후 제거 예정.
     */
    static LearningAxis create(LearningFacade facade, String name, int displayOrder) {
        requireNonNull(facade, "facade");
        validateName(name);

        if (displayOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 1 이상이어야 합니다. displayOrder=" + displayOrder
            );
        }
        return new LearningAxis(facade, null, name.trim(), displayOrder);
    }

    /**
     * Layer 소속 axis 생성 (Story-LT-E2-S2 blessed 경로).
     * {@link LearningLayer#addAxis(String)}가 호출한다.
     */
    static LearningAxis createInLayer(LearningFacade facade, LearningLayer layer, String name, int displayOrder) {
        requireNonNull(facade, "facade");
        requireNonNull(layer, "layer");
        validateName(name);
        if (displayOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 1 이상이어야 합니다. displayOrder=" + displayOrder
            );
        }
        return new LearningAxis(facade, layer, name.trim(), displayOrder);
    }

    /**
     * Layer 소속을 사후 주입 (Story-LT-E2-S3 default layer 라우팅).
     * 기존에 layer가 없던 axis에만 사용 — 이미 layer가 지정됐다면 no-op.
     */
    void assignLayer(LearningLayer layer) {
        if (this.layer == null) {
            this.layer = layer;
        }
    }

    public void updateName(String newName) {
        validateName(newName);
        this.name = newName.trim();
    }

    /**
     * 축 논리 삭제.
     * {@code deleted_at}을 현재 시각으로 설정한다. 클래스 상단 {@code @SQLRestriction}으로
     * 이후 모든 read 쿼리에서 자동 제외된다.
     *
     * <p>이미 삭제된 축을 재삭제하면 {@link ErrorCode#LEARNING_AXIS_ALREADY_DELETED} 예외.
     * (Deck.softDelete()의 DECK_ALREADY_DELETED와 동일 패턴)
     *
     * <p>본 메서드는 자기 자신의 상태만 변경한다. 축에 속한 Deck 연쇄 소프트 삭제는
     * Application Service(LearningFacadeCommandService.removeAxis)가 조율한다.
     */
    public void softDelete() {
        if (this.deletedAt != null) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_ALREADY_DELETED);
        }
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    void updateDisplayOrder(int newOrder) {
        if (newOrder < 1) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "displayOrder는 1 이상이어야 합니다. newOrder=" + newOrder
            );
        }
        this.displayOrder = newOrder;
    }

    // ─── Deck 폐기 흡수 도메인 행위 (LT E5 · M5 · Story 5-1) ─────────

    /**
     * NOT_STARTED → IN_PROGRESS 전용 마커.
     * 축에 첫 Card가 추가될 때 Application Service가 호출한다.
     *
     * <p><strong>멱등</strong>: 이미 IN_PROGRESS / COMPLETED이면 무시.
     * COMPLETED → IN_PROGRESS 회귀는 본 메서드 책임 아님. {@link #recalculateProgressStatus(int, int)} 담당.
     *
     * <p>(Deck.markInProgress() 계승 · LT E5)
     */
    public void markInProgress() {
        if (this.progressStatus == AxisProgressStatus.NOT_STARTED) {
            this.progressStatus = AxisProgressStatus.IN_PROGRESS;
        }
    }

    /**
     * Card 상태 집계 결과로 progressStatus 재계산.
     * <p>LearningAxis는 자기 자신의 자식 컬렉션 (topics·roadmapNodes·selections) 만 소유하므로
     * Card 컬렉션을 직접 순회하지 않는다. Application Service (CardCommandService 등)가
     * 카드 카운트를 계산한 후 본 메서드를 호출.
     *
     * <ul>
     *   <li>활성 + 아카이브 Card 0개 → NOT_STARTED</li>
     *   <li>활성 Card 0개 + 아카이브 Card ≥1 → COMPLETED</li>
     *   <li>그 외 → IN_PROGRESS</li>
     * </ul>
     *
     * <p>(Deck.recalculateProgressStatus() 계승 · LT E5 · Epic 6 Story 6-4 연결점)
     */
    public void recalculateProgressStatus(int activeCardCount, int archivedCardCount) {
        if (activeCardCount == 0 && archivedCardCount == 0) {
            this.progressStatus = AxisProgressStatus.NOT_STARTED;
            return;
        }
        boolean allArchived = activeCardCount == 0 && archivedCardCount > 0;
        this.progressStatus = allArchived
                ? AxisProgressStatus.COMPLETED
                : AxisProgressStatus.IN_PROGRESS;
    }

    /**
     * 최근 접근 시각 갱신 (Deck.updateLastAccessed() 계승).
     * Review 세션 진입 또는 축 조회 시 Application Service가 호출.
     */
    public void updateLastAccessed() {
        this.lastAccessedAt = LocalDateTime.now();
    }

    /**
     * 학습 모드 변경 (Deck.changeMode() 계승 · SDD 우선 재정의).
     * null 검증. 값 자체는 동일 시에도 대입 (updated_at 갱신 트리거).
     */
    public void changeMode(AxisLearningMode newMode) {
        if (newMode == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    "AxisLearningMode는 null일 수 없습니다."
            );
        }
        this.mode = newMode;
    }

    /**
     * 원천 학습 자료 삭제 시 호출 (Deck.markMaterialDeleted() 계승).
     * {@code learningMaterialId}만 null로 전환 — "자료 미연결 Axis"로 유지.
     *
     * <p><strong>멱등</strong>: 이미 null이면 효과 없음 (예외 X).
     */
    public void markMaterialDeleted() {
        this.learningMaterialId = null;
    }

    public AxisTopic addTopic(String name, String description) {
        int nextOrder = topics.size() + 1;
        AxisTopic topic = AxisTopic.create(this, name, description, nextOrder);
        topics.add(topic);
        return topic;
    }

    /**
     * 다건 주제 추가 (AI 제안 체크박스 등). 입력 순서대로 displayOrder가 1-based로 부여된다.
     */
    public List<AxisTopic> addTopics(List<TopicCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return List.of();
        }
        List<AxisTopic> added = new ArrayList<>(commands.size());
        for (TopicCommand cmd : commands) {
            added.add(addTopic(cmd.name(), cmd.description()));
        }
        return added;
    }

    public void removeTopic(Long topicId) {
        AxisTopic target = findTopic(topicId);
        topics.remove(target);
    }

    /**
     * 주제 순서 변경. 전달된 id 순서대로 displayOrder를 1-based로 재부여한다.
     * 전달된 id 집합이 현재 topics와 일치해야 한다.
     */
    public void reorderTopics(List<Long> orderedTopicIds) {
        validateReorderTopicIds(orderedTopicIds);
        IntStream.range(0, orderedTopicIds.size()).forEach(i -> {
            AxisTopic topic = findTopic(orderedTopicIds.get(i));
            topic.updateDisplayOrder(i + 1);
        });
    }

    public boolean isTopicCountExceedsRecommended() {
        return topics.size() > RECOMMENDED_TOPIC_LIMIT;
    }

    /**
     * 이 축에 속한 주제 중 자료 미연결 주제가 하나라도 있는지.
     * 축 카드 ⚠️ 경고 뱃지 표시 판단에 사용한다.
     */
    public boolean hasUncoveredTopics() {
        return topics.stream().anyMatch(AxisTopic::isUncovered);
    }

    /**
     * 이 축에 속한 미커버 주제 개수.
     */
    public int countUncoveredTopics() {
        return (int) topics.stream().filter(AxisTopic::isUncovered).count();
    }

    /**
     * 주제가 1개 이상이고 모든 주제가 자료로 커버되어 있으면 true.
     * 빈 축(주제 0개)은 false. PARTIAL 상태 주제만 있어도 미커버가 없으면 완전 커버로 간주한다
     * (도메인 스펙 §13 디폴트 — coverage != NO_MATERIAL 기준).
     */
    public boolean isFullyCovered() {
        return !topics.isEmpty() && topics.stream().noneMatch(AxisTopic::isUncovered);
    }

    public List<AxisTopic> getTopics() {
        return Collections.unmodifiableList(topics);
    }

    public AxisTopic findTopic(Long topicId) {
        return topics.stream()
                .filter(t -> t.getId() != null && t.getId().equals(topicId))
                .findFirst()
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.LEARNING_AXIS_TOPIC_NOT_FOUND,
                        "topicId=" + topicId
                ));
    }

    private void validateReorderTopicIds(List<Long> orderedTopicIds) {
        if (orderedTopicIds == null) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_TOPIC_REORDER_MISMATCH);
        }
        Set<Long> currentIds = topics.stream()
                .map(AxisTopic::getId)
                .collect(Collectors.toSet());

        if (orderedTopicIds.size() != currentIds.size()) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_AXIS_TOPIC_REORDER_MISMATCH,
                    "전달된 주제 id 수(" + orderedTopicIds.size()
                            + ")가 현재 topics 수(" + currentIds.size() + ")와 다릅니다."
            );
        }

        Set<Long> incomingIds = new HashSet<>(orderedTopicIds);
        if (!currentIds.equals(incomingIds)) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.LEARNING_AXIS_TOPIC_REORDER_MISMATCH,
                    "전달된 주제 id 목록이 현재 topics id 집합과 일치하지 않습니다."
            );
        }
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.LEARNING_AXIS_NAME_BLANK);
        }
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.INVALID_INPUT,
                    fieldName + "은(는) null일 수 없습니다."
            );
        }
    }

    public record TopicCommand(String name, String description) {
        public static TopicCommand of(String name, String description) {
            return new TopicCommand(name, description);
        }
    }

    // ─── Roadmap 노드 도메인 API (Story-LT-E3-S3-6, 이슈 #15) ─────────

    /**
     * Roadmap 챕터 노드 추가. display_order는 (활성 노드 max) + 1.
     *
     * @return 신규 노드 (id는 flush 후 부여)
     */
    public AxisRoadmapNode addRoadmapNode(String title, String rationale, String body) {
        int nextOrder = roadmapNodes.size() + 1;
        AxisRoadmapNode node = AxisRoadmapNode.create(this, nextOrder, title, rationale, body);
        roadmapNodes.add(node);
        return node;
    }

    /**
     * Roadmap 노드 순서 재부여. 전달된 id 순서대로 displayOrder를 1-based로 재부여한다.
     * 전달된 id 집합이 현재 활성 roadmapNodes와 불일치하면 예외.
     */
    public void reorderRoadmapNodes(List<Long> orderedNodeIds) {
        validateRoadmapNodeReorderIds(orderedNodeIds);
        IntStream.range(0, orderedNodeIds.size()).forEach(i -> {
            AxisRoadmapNode node = findRoadmapNode(orderedNodeIds.get(i));
            node.updateDisplayOrder(i + 1);
        });
    }

    /**
     * Roadmap 노드 소프트 삭제. 노드 자체 {@link AxisRoadmapNode#softDelete()} 호출.
     * 재삭제는 no-op (멱등).
     */
    public void removeRoadmapNode(Long nodeId) {
        AxisRoadmapNode target = findRoadmapNode(nodeId);
        target.softDelete();
    }

    public AxisRoadmapNode findRoadmapNode(Long nodeId) {
        return roadmapNodes.stream()
                .filter(n -> n.getId() != null && n.getId().equals(nodeId))
                .findFirst()
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.ROADMAP_NODE_NOT_FOUND,
                        "nodeId=" + nodeId));
    }

    /**
     * 활성 Roadmap 노드 목록 (soft delete된 노드는 {@code @SQLRestriction}에 의해 자동 제외).
     */
    public List<AxisRoadmapNode> getRoadmapNodes() {
        return Collections.unmodifiableList(roadmapNodes);
    }

    private void validateRoadmapNodeReorderIds(List<Long> orderedNodeIds) {
        if (orderedNodeIds == null) {
            throw LearningFacadeDomainException.of(ErrorCode.ROADMAP_NODE_ORDER_MISMATCH);
        }
        Set<Long> currentIds = roadmapNodes.stream()
                .map(AxisRoadmapNode::getId)
                .collect(Collectors.toSet());

        if (orderedNodeIds.size() != currentIds.size()) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.ROADMAP_NODE_ORDER_MISMATCH,
                    "전달된 노드 id 수(" + orderedNodeIds.size()
                            + ")가 현재 노드 수(" + currentIds.size() + ")와 다릅니다.");
        }

        Set<Long> incomingIds = new HashSet<>(orderedNodeIds);
        if (!currentIds.equals(incomingIds)) {
            throw LearningFacadeDomainException.of(
                    ErrorCode.ROADMAP_NODE_ORDER_MISMATCH,
                    "전달된 노드 id 목록이 현재 노드 id 집합과 일치하지 않습니다.");
        }
    }

    // ─── Selection 컨테이너 도메인 API (Story-LT-E3-S3-9, 이슈 #16) ─────────
    //
    // 이슈 #11 정책 계승:
    //   · name UNIQUE per axis (활성 컨테이너)
    //   · created_at DESC 정렬 (@OrderBy로 컬렉션 로딩 시 자동)
    //   · hard delete (removeSelection 시 즉시 삭제)
    //
    // 자식 노드 CRUD는 {@link AxisSelection#addNode/reorderNodes/removeNode}가 직접 담당.

    /**
     * Selection 컨테이너 추가. 동일 이름 중복 시 예외.
     *
     * <p>이슈 #11 정책 계승: name UNIQUE per axis (활성 컨테이너 대상).
     * name blank 검증은 {@link AxisSelection#create}에서 최종 담당하지만, 중복 감지 정확도를
     * 위해 여기서 미리 trim 한 상태로 비교한다.
     */
    public AxisSelection addSelection(String name) {
        if (name == null || name.isBlank()) {
            throw LearningFacadeDomainException.of(ErrorCode.AXIS_SELECTION_NAME_BLANK);
        }
        String trimmed = name.trim();
        boolean duplicate = selections.stream()
                .anyMatch(s -> s.getName().equals(trimmed));
        if (duplicate) {
            throw LearningFacadeDomainException.of(ErrorCode.AXIS_SELECTION_NAME_ALREADY_EXISTS);
        }
        AxisSelection selection = AxisSelection.create(this, trimmed);
        selections.add(selection);
        return selection;
    }

    /**
     * Selection 컨테이너 이름 변경 (in-place, 이슈 #11 계승).
     * 다른 활성 컨테이너와 중복 시 예외.
     */
    public void renameSelection(Long selectionId, String newName) {
        AxisSelection target = findSelection(selectionId);
        String trimmed = newName == null ? null : newName.trim();
        boolean duplicate = selections.stream()
                .filter(s -> !s.getId().equals(selectionId))
                .anyMatch(s -> s.getName().equals(trimmed));
        if (duplicate) {
            throw LearningFacadeDomainException.of(ErrorCode.AXIS_SELECTION_NAME_ALREADY_EXISTS);
        }
        target.updateName(newName);
    }

    /**
     * Selection 컨테이너 hard delete. orphanRemoval=true로 자식 노드 CASCADE 삭제.
     */
    public void removeSelection(Long selectionId) {
        AxisSelection target = findSelection(selectionId);
        selections.remove(target);
    }

    public AxisSelection findSelection(Long selectionId) {
        return selections.stream()
                .filter(s -> s.getId() != null && s.getId().equals(selectionId))
                .findFirst()
                .orElseThrow(() -> LearningFacadeDomainException.of(
                        ErrorCode.AXIS_SELECTION_NOT_FOUND,
                        "selectionId=" + selectionId));
    }

    /**
     * 활성 Selection 컨테이너 목록 (created_at DESC).
     */
    public List<AxisSelection> getSelections() {
        return Collections.unmodifiableList(selections);
    }
}
