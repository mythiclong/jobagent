package com.zhihang.jobagent.service.update.question;

public record FetchedInterviewQuestionItem(
        String sourceName,
        String sourceUrl,
        String questionTitle,
        String questionText,
        String topicTag,
        String sourceSummary,
        String questionHash
) {
}
