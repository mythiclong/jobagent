package com.zhihang.jobagent.dto;

public class LearningPathCoachGenerationResult {

    private final LearningPathCoachAdvice advice;
    private final AiGenerationMetadata metadata;

    public LearningPathCoachGenerationResult(LearningPathCoachAdvice advice, AiGenerationMetadata metadata) {
        this.advice = advice;
        this.metadata = metadata;
    }

    public LearningPathCoachAdvice getAdvice() {
        return advice;
    }

    public AiGenerationMetadata getMetadata() {
        return metadata;
    }
}
