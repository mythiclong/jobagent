package com.zhihang.jobagent.dto;

import com.zhihang.jobagent.entity.InterviewQuestion;

import java.util.ArrayList;
import java.util.List;

public class InterviewQuestionGenerationResult {

    private final List<InterviewQuestion> acceptedQuestions;
    private final boolean aiAttempted;
    private final boolean aiSupplemented;
    private final String providerName;
    private final String noticeMessage;

    public InterviewQuestionGenerationResult(List<InterviewQuestion> acceptedQuestions,
                                             boolean aiAttempted,
                                             boolean aiSupplemented,
                                             String providerName,
                                             String noticeMessage) {
        this.acceptedQuestions = acceptedQuestions == null ? List.of() : List.copyOf(acceptedQuestions);
        this.aiAttempted = aiAttempted;
        this.aiSupplemented = aiSupplemented;
        this.providerName = providerName == null ? "" : providerName;
        this.noticeMessage = noticeMessage == null ? "" : noticeMessage;
    }

    public static InterviewQuestionGenerationResult noAttempt() {
        return new InterviewQuestionGenerationResult(List.of(), false, false, "", "");
    }

    public static InterviewQuestionGenerationResult fallbackNotice(String noticeMessage) {
        return new InterviewQuestionGenerationResult(List.of(), true, false, "", noticeMessage);
    }

    public List<InterviewQuestion> getAcceptedQuestions() {
        return new ArrayList<>(acceptedQuestions);
    }

    public boolean isAiAttempted() {
        return aiAttempted;
    }

    public boolean isAiSupplemented() {
        return aiSupplemented;
    }

    public String getProviderName() {
        return providerName;
    }

    public String getNoticeMessage() {
        return noticeMessage;
    }
}
