package com.zhihang.jobagent.dto;

public class ResumeAiGenerationResult {

    private final ResumeAiReviewResult review;
    private final AiGenerationMetadata metadata;

    public ResumeAiGenerationResult(ResumeAiReviewResult review, AiGenerationMetadata metadata) {
        this.review = review;
        this.metadata = metadata;
    }

    public ResumeAiReviewResult getReview() {
        return review;
    }

    public AiGenerationMetadata getMetadata() {
        return metadata;
    }
}
