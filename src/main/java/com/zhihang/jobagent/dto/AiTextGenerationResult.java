package com.zhihang.jobagent.dto;

public class AiTextGenerationResult {

    private final String text;
    private final AiGenerationMetadata metadata;

    public AiTextGenerationResult(String text, AiGenerationMetadata metadata) {
        this.text = text;
        this.metadata = metadata;
    }

    public String getText() {
        return text;
    }

    public AiGenerationMetadata getMetadata() {
        return metadata;
    }

    public String getContent() {
        return text;
    }

    public GenerationSource getSource() {
        return metadata == null ? GenerationSource.FALLBACK : metadata.getGenerationSource();
    }

    public String getModelName() {
        return metadata == null ? "" : metadata.getModelName();
    }

    public boolean isSuccess() {
        return metadata != null && metadata.isAiUsed();
    }

    public String getFailureReason() {
        if (metadata == null) {
            return "";
        }
        if (metadata.getGenerationSource() == GenerationSource.FALLBACK) {
            return metadata.getFallbackReason();
        }
        return metadata.getPrimaryFailureReason();
    }

    public String getPrimaryFailureReason() {
        return metadata == null ? "" : metadata.getPrimaryFailureReason();
    }

    public String getSecondaryFailureReason() {
        return metadata == null ? "" : metadata.getSecondaryFailureReason();
    }

    public boolean isTimeoutOccurred() {
        return metadata != null && metadata.isTimeoutOccurred();
    }
}
