package com.zhihang.jobagent.agent.prompt;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptLoaderTest {

    private final PromptLoader loader = new PromptLoader(new DefaultResourceLoader());

    @Test
    void loadsAllVersionedPromptDocuments() {
        assertThat(loader.load(PromptLoader.SYSTEM))
                .contains("职航 Agent")
                .contains("prompt-version: v1");
        assertThat(loader.load(PromptLoader.RESUME_ANALYSIS))
                .contains("{jobName}")
                .contains("{resumeText}");
        assertThat(loader.load(PromptLoader.OUTPUT_SCHEMA))
                .contains("coreProblems")
                .contains("improvementSuggestions");
    }

    @Test
    void readsPromptVersionsForTraceMetadata() {
        assertThat(loader.version(PromptLoader.SYSTEM)).isEqualTo("v1");
        assertThat(loader.version(PromptLoader.RESUME_ANALYSIS)).isEqualTo("v1");
        assertThat(loader.version(PromptLoader.OUTPUT_SCHEMA)).isEqualTo("v1");
    }

    @Test
    void rejectsUnknownPromptNames() {
        assertThatThrownBy(() -> loader.load("../application.properties"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Agent prompt");
    }

    @Test
    void reportsMissingPromptResourcesClearly() {
        PromptLoader missingLoader = new PromptLoader(
                new DefaultResourceLoader() {
                    @Override
                    public org.springframework.core.io.Resource getResource(String location) {
                        return new org.springframework.core.io.ClassPathResource("prompts/not-found.md");
                    }
                }
        );

        assertThatThrownBy(() -> missingLoader.load(PromptLoader.SYSTEM))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Agent prompt resource is missing");
    }
}
