package com.zhihang.jobagent.service;

import com.zhihang.jobagent.dto.AiGenerationMetadata;
import com.zhihang.jobagent.dto.AiTextGenerationResult;
import com.zhihang.jobagent.entity.InterviewQuestion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class InterviewQuestionTranslationServiceTests {

    private final AiOrchestratorService aiOrchestratorService = mock(AiOrchestratorService.class);
    private final ContentNormalizationService contentNormalizationService = new ContentNormalizationService();
    private final InterviewQuestionTranslationService translationService = new InterviewQuestionTranslationService(
            aiOrchestratorService,
            contentNormalizationService,
            true,
            8
    );

    @Test
    void shouldTranslateEnglishQuestionTitleInBatch() {
        InterviewQuestion question = new InterviewQuestion();
        question.setRawTitle("Why do you need a Cookie?");
        question.setQuestionText("Why do you need a Cookie?");
        question.setJobDirection("前端开发");

        String json = """
                {
                  "translations": [
                    {"index": 0, "title": "为什么需要 Cookie？"}
                  ]
                }
                """;
        given(aiOrchestratorService.isGeminiConfigured()).willReturn(true);
        given(aiOrchestratorService.generateJson(anyString(), eq("interview-question-translate"), anyFloat()))
                .willReturn(new AiTextGenerationResult(json, AiGenerationMetadata.gemini("gemini-test")));

        translationService.translateTitlesIfNeeded(List.of(question));

        assertThat(question.getDisplayTitle()).isEqualTo("为什么需要 Cookie？");
    }
}
