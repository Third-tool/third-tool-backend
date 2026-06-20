package com.example.thirdtool.LearningFacade.application.port.out.suggestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 도메인 계층이 Suggestion Port·VO에 의존하지 않음을 검증한다.
 *
 * <p>ArchUnit 의존 미설치 — Story 1-1 DoD가 허용하는 grep 기반 검증으로 대체.
 * Hexagonal 원칙: 도메인은 외부 시스템(LLM 등)의 존재를 모른다.</p>
 */
@DisplayName("Suggestion Port 도메인 격리 (grep 기반)")
class SuggestionPortDomainIsolationTest {

    private static final Path DOMAIN_ROOT = Path.of(
            "src", "main", "java",
            "com", "example", "thirdtool",
            "LearningFacade", "domain"
    );

    private static final Pattern FORBIDDEN = Pattern.compile(
            "\\b(AxisSuggestionPort|AxisTopicSuggestionPort|SuggestionConceptContext|AxisSuggestion|AxisTopicSuggestion)\\b"
    );

    @Test
    @DisplayName("LearningFacade/domain/ 하위 어떤 .java 파일에도 Suggestion Port·VO 참조가 없다")
    void domain_layer_has_no_suggestion_reference() throws IOException {
        assertThat(DOMAIN_ROOT).exists();

        List<String> violations = new ArrayList<>();
        Files.walkFileTree(DOMAIN_ROOT, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!file.toString().endsWith(".java")) {
                    return FileVisitResult.CONTINUE;
                }
                String content = Files.readString(file);
                if (FORBIDDEN.matcher(content).find()) {
                    violations.add(file.toString());
                }
                return FileVisitResult.CONTINUE;
            }
        });

        assertThat(violations)
                .as("도메인 계층은 Suggestion Port·VO를 import/참조하지 않아야 한다.")
                .isEmpty();
    }
}
