package com.example.thirdtool.LearningFacade.infrastructure.suggestion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Story-AS-E7-S7-1 · 프롬프트 템플릿 파일 존재 및 필수 지시 검증.
 * v1은 Static Adapter만 활성이므로 파일 로드는 이 테스트에서만 검증한다.
 */
@DisplayName("Prompt Templates 배치 검증 (Story-AS-E7-S7-1)")
class PromptTemplatesPresenceTest {

    @Test
    @DisplayName("concept-spec.txt 존재 + 카탈로그 6종 · 5종 문구 포함")
    void conceptSpec_present() throws IOException {
        String content = readClasspathText("prompts/concept-spec.txt");

        assertThat(content).contains("Roadmap 콘텐츠 카탈로그");
        assertThat(content).contains("Selections 콘텐츠 카탈로그");
        assertThat(content).contains("수렴 point 판별");
        assertThat(content).contains("발산 point 판별");
    }

    @Test
    @DisplayName("chapters-outline.txt 존재 + concept-spec include + roadmap 지시")
    void chaptersOutline_present() throws IOException {
        String content = readClasspathText("prompts/chapters-outline.txt");

        assertThat(content).contains("{{include:concept-spec.txt}}");
        assertThat(content).contains("도구 이름·특정 옵션 비교는 금지");
    }

    @Test
    @DisplayName("chapter-subtree.txt 존재 + roadmap 지시")
    void chapterSubtree_present() throws IOException {
        String content = readClasspathText("prompts/chapter-subtree.txt");

        assertThat(content).contains("{{include:concept-spec.txt}}");
        assertThat(content).contains("bodyAsciiTree");
    }

    @Test
    @DisplayName("selection-outline.txt 존재 + selections 지시 (기준은 roadmap 담당)")
    void selectionOutline_present() throws IOException {
        String content = readClasspathText("prompts/selection-outline.txt");

        assertThat(content).contains("{{include:concept-spec.txt}}");
        assertThat(content).contains("기준은 roadmap에 있다고 가정");
    }

    @Test
    @DisplayName("selection-subtree.txt 존재 + 사례/비교 지시")
    void selectionSubtree_present() throws IOException {
        String content = readClasspathText("prompts/selection-subtree.txt");

        assertThat(content).contains("{{include:concept-spec.txt}}");
        assertThat(content).contains("사례·비교");
    }

    @Test
    @DisplayName("layer.txt · axis.txt 존재")
    void layerAndAxis_present() throws IOException {
        assertThat(readClasspathText("prompts/layer.txt")).contains("Layer 후보");
        assertThat(readClasspathText("prompts/axis.txt")).contains("Axis 후보");
    }

    private static String readClasspathText(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        assertThat(resource.exists()).as("classpath에 %s가 존재해야 합니다.", path).isTrue();
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
