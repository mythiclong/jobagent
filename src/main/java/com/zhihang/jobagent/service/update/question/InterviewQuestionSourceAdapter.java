package com.zhihang.jobagent.service.update.question;

import java.io.IOException;
import java.util.List;

public interface InterviewQuestionSourceAdapter {

    String getSourceName();

    boolean isEnabled();

    List<FetchedInterviewQuestionItem> fetchQuestions() throws IOException;
}
