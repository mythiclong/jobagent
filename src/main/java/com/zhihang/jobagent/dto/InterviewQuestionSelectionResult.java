package com.zhihang.jobagent.dto;

import com.zhihang.jobagent.entity.InterviewQuestion;

import java.util.ArrayList;
import java.util.List;

public class InterviewQuestionSelectionResult {

    private final List<InterviewQuestion> questions;
    private final String noticeMessage;
    private final int relevantQuestionCount;
    private final int generatedQuestionCount;

    public InterviewQuestionSelectionResult(List<InterviewQuestion> questions,
                                            String noticeMessage,
                                            int relevantQuestionCount,
                                            int generatedQuestionCount) {
        this.questions = questions == null ? List.of() : List.copyOf(questions);
        this.noticeMessage = noticeMessage == null ? "" : noticeMessage;
        this.relevantQuestionCount = relevantQuestionCount;
        this.generatedQuestionCount = generatedQuestionCount;
    }

    public List<InterviewQuestion> getQuestions() {
        return new ArrayList<>(questions);
    }

    public String getNoticeMessage() {
        return noticeMessage;
    }

    public int getRelevantQuestionCount() {
        return relevantQuestionCount;
    }

    public int getGeneratedQuestionCount() {
        return generatedQuestionCount;
    }
}
