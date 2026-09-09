package com.zhihang.jobagent.dto;

public class AiGenerationMetadata {

    private final GenerationSource generationSource;
    private final boolean aiUsed;
    private final String fallbackReason;
    private final String modelName;
    private final String primaryFailureReason;
    private final String secondaryFailureReason;
    private final boolean timeoutOccurred;
    private final AiFailureLayer primaryFailureLayer;
    private final AiFailureType primaryFailureType;
    private final AiFailureLayer secondaryFailureLayer;
    private final AiFailureType secondaryFailureType;

    public AiGenerationMetadata(GenerationSource generationSource, boolean aiUsed, String fallbackReason, String modelName) {
        this(
                generationSource,
                aiUsed,
                fallbackReason,
                modelName,
                "",
                "",
                false,
                AiFailureLayer.NONE,
                AiFailureType.NONE,
                AiFailureLayer.NONE,
                AiFailureType.NONE
        );
    }

    public AiGenerationMetadata(GenerationSource generationSource,
                                boolean aiUsed,
                                String fallbackReason,
                                String modelName,
                                String primaryFailureReason,
                                String secondaryFailureReason,
                                boolean timeoutOccurred,
                                AiFailureLayer primaryFailureLayer,
                                AiFailureType primaryFailureType,
                                AiFailureLayer secondaryFailureLayer,
                                AiFailureType secondaryFailureType) {
        this.generationSource = generationSource;
        this.aiUsed = aiUsed;
        this.fallbackReason = fallbackReason;
        this.modelName = modelName;
        this.primaryFailureReason = primaryFailureReason;
        this.secondaryFailureReason = secondaryFailureReason;
        this.timeoutOccurred = timeoutOccurred;
        this.primaryFailureLayer = primaryFailureLayer;
        this.primaryFailureType = primaryFailureType;
        this.secondaryFailureLayer = secondaryFailureLayer;
        this.secondaryFailureType = secondaryFailureType;
    }

    public static AiGenerationMetadata gemini(String modelName) {
        return gemini(
                modelName,
                "",
                AiFailureLayer.NONE,
                AiFailureType.NONE
        );
    }

    public static AiGenerationMetadata gemini(String modelName,
                                              String primaryFailureReason,
                                              AiFailureLayer primaryFailureLayer,
                                              AiFailureType primaryFailureType) {
        return new AiGenerationMetadata(
                GenerationSource.GEMINI,
                true,
                "",
                modelName,
                primaryFailureReason,
                "",
                false,
                primaryFailureLayer,
                primaryFailureType,
                AiFailureLayer.NONE,
                AiFailureType.NONE
        );
    }

    public static AiGenerationMetadata qwen(String modelName) {
        return qwen(
                modelName,
                "",
                AiFailureLayer.NONE,
                AiFailureType.NONE
        );
    }

    public static AiGenerationMetadata qwen(String modelName, String primaryFailureReason) {
        return qwen(
                modelName,
                primaryFailureReason,
                AiFailureLayer.UNKNOWN,
                AiFailureType.UNKNOWN_EXCEPTION
        );
    }

    public static AiGenerationMetadata qwen(String modelName,
                                            String primaryFailureReason,
                                            AiFailureLayer primaryFailureLayer,
                                            AiFailureType primaryFailureType) {
        return new AiGenerationMetadata(
                GenerationSource.QWEN,
                true,
                "",
                modelName,
                primaryFailureReason,
                "",
                false,
                primaryFailureLayer,
                primaryFailureType,
                AiFailureLayer.NONE,
                AiFailureType.NONE
        );
    }

    public static AiGenerationMetadata fallback(String modelName, String fallbackReason) {
        return fallback(
                modelName,
                fallbackReason,
                "",
                "",
                false,
                AiFailureLayer.NONE,
                AiFailureType.NONE,
                AiFailureLayer.NONE,
                AiFailureType.NONE
        );
    }

    public static AiGenerationMetadata fallback(String modelName,
                                                String fallbackReason,
                                                String primaryFailureReason,
                                                String secondaryFailureReason,
                                                boolean timeoutOccurred) {
        return fallback(
                modelName,
                fallbackReason,
                primaryFailureReason,
                secondaryFailureReason,
                timeoutOccurred,
                AiFailureLayer.UNKNOWN,
                AiFailureType.UNKNOWN_EXCEPTION,
                AiFailureLayer.UNKNOWN,
                AiFailureType.UNKNOWN_EXCEPTION
        );
    }

    public static AiGenerationMetadata fallback(String modelName,
                                                String fallbackReason,
                                                String primaryFailureReason,
                                                String secondaryFailureReason,
                                                boolean timeoutOccurred,
                                                AiFailureLayer primaryFailureLayer,
                                                AiFailureType primaryFailureType,
                                                AiFailureLayer secondaryFailureLayer,
                                                AiFailureType secondaryFailureType) {
        return new AiGenerationMetadata(
                GenerationSource.FALLBACK,
                false,
                fallbackReason,
                modelName,
                primaryFailureReason,
                secondaryFailureReason,
                timeoutOccurred,
                primaryFailureLayer,
                primaryFailureType,
                secondaryFailureLayer,
                secondaryFailureType
        );
    }

    public GenerationSource getGenerationSource() {
        return generationSource;
    }

    public boolean isAiUsed() {
        return aiUsed;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public String getModelName() {
        return modelName;
    }

    public String getPrimaryFailureReason() {
        return primaryFailureReason;
    }

    public String getSecondaryFailureReason() {
        return secondaryFailureReason;
    }

    public boolean isTimeoutOccurred() {
        return timeoutOccurred;
    }

    public AiFailureLayer getPrimaryFailureLayer() {
        return primaryFailureLayer;
    }

    public AiFailureType getPrimaryFailureType() {
        return primaryFailureType;
    }

    public AiFailureLayer getSecondaryFailureLayer() {
        return secondaryFailureLayer;
    }

    public AiFailureType getSecondaryFailureType() {
        return secondaryFailureType;
    }
}
