package com.zhihang.jobagent.service;

import com.zhihang.jobagent.entity.InterviewQuestion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentNormalizationServiceTests {

    private final ContentNormalizationService contentNormalizationService = new ContentNormalizationService();

    @Test
    void shouldNormalizeEnglishActionQuestionWithoutKeepingListNumberOrYouFragment() {
        InterviewQuestion question = new InterviewQuestion();
        question.setRawTitle("3. How do you access web storage?");
        question.setQuestionText("3. How do you access web storage?");
        question.setQuestionType("基础知识");
        question.setJobDirection("前端开发");
        question.setNormalizedStatus("OK");

        contentNormalizationService.normalizeInterviewQuestion(question);

        assertThat(question.getDisplayTitle()).doesNotStartWith("3.");
        assertThat(question.getDisplayTitle().toLowerCase()).contains("web storage");
        assertThat(question.getDisplayTitle()).contains("访问");
        assertThat(question.getDisplayTitle().toLowerCase()).doesNotContain("you access");
        assertThat(question.getDisplayTitle()).doesNotContain("是如何工作的");
    }

    @Test
    void shouldKeepMechanismQuestionAsHowItWorksQuestion() {
        InterviewQuestion question = new InterviewQuestion();
        question.setRawTitle("How does event loop work?");
        question.setQuestionText("How does event loop work?");
        question.setQuestionType("基础知识");
        question.setJobDirection("前端开发");

        contentNormalizationService.normalizeInterviewQuestion(question);

        assertThat(question.getDisplayTitle()).contains("事件循环");
        assertThat(question.getDisplayTitle()).contains("是如何工作的");
    }

    @Test
    void shouldMarkBrokenEnglishPromptForRefresh() {
        InterviewQuestion question = new InterviewQuestion();
        question.setRawTitle("How do you access web storage?");
        question.setQuestionText("How do you access web storage?");
        question.setDisplayTitle("3. you access web storage是如何工作的？");
        question.setNormalizedStatus("OK");

        assertThat(contentNormalizationService.shouldRefreshInterviewQuestion(question)).isTrue();
    }
    @Test
    void shouldNormalizeWhyNeedQuestionWithoutLeadingNoise() {
        InterviewQuestion question = new InterviewQuestion();
        question.setRawTitle(". Why do you need a Cookie?");
        question.setQuestionText(". Why do you need a Cookie?");
        question.setQuestionType("基础知识");
        question.setJobDirection("前端开发");

        contentNormalizationService.normalizeInterviewQuestion(question);

        assertThat(question.getDisplayTitle()).isEqualTo("为什么需要Cookie？");
    }
}
