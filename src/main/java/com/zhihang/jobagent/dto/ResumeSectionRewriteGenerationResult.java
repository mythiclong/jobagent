package com.zhihang.jobagent.dto;

public class ResumeSectionRewriteGenerationResult {

    private final String rewritten;
    private final String reason;
    private final AiGenerationMetadata metadata;

    public ResumeSectionRewriteGenerationResult(String rewritten, String reason, AiGenerationMetadata metadata) {
        this.rewritten = rewritten;
        this.reason = reason;
        this.metadata = metadata;
    }

    public String getRewritten() {
        return rewritten;
    }

    public String getReason() {
        return reason;
    }

    public AiGenerationMetadata getMetadata() {
        return metadata;
    }
}
