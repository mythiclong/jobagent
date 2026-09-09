package com.zhihang.jobagent.service;

import com.zhihang.jobagent.dto.AiFailureLayer;
import com.zhihang.jobagent.dto.AiFailureType;
import com.zhihang.jobagent.dto.GenerationSource;

public class AiProviderResult {

    private final GenerationSource source;
    private final boolean success;
    private final String content;
    private final String modelName;
    private final String failureReason;
    private final boolean timeoutOccurred;
    private final AiFailureLayer failureLayer;
    private final AiFailureType failureType;

    private AiProviderResult(GenerationSource source,
                             boolean success,
                             String content,
                             String modelName,
                             String failureReason,
                             boolean timeoutOccurred,
                             AiFailureLayer failureLayer,
                             AiFailureType failureType) {
        this.source = source;
        this.success = success;
        this.content = content;
        this.modelName = modelName;
        this.failureReason = failureReason;
        this.timeoutOccurred = timeoutOccurred;
        this.failureLayer = failureLayer;
        this.failureType = failureType;
    }

    public static AiProviderResult success(GenerationSource source, String content, String modelName) {
        return new AiProviderResult(source, true, content, modelName, "", false, AiFailureLayer.NONE, AiFailureType.NONE);
    }

    public static AiProviderResult failure(GenerationSource source, String modelName, String failureReason, boolean timeoutOccurred) {
        return failure(source, modelName, failureReason, timeoutOccurred, AiFailureLayer.UNKNOWN, AiFailureType.UNKNOWN_EXCEPTION);
    }

    public static AiProviderResult failure(GenerationSource source,
                                           String modelName,
                                           String failureReason,
                                           boolean timeoutOccurred,
                                           AiFailureLayer failureLayer,
                                           AiFailureType failureType) {
        return new AiProviderResult(source, false, "", modelName, failureReason, timeoutOccurred, failureLayer, failureType);
    }

    public GenerationSource getSource() {
        return source;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getContent() {
        return content;
    }

    public String getModelName() {
        return modelName;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public boolean isTimeoutOccurred() {
        return timeoutOccurred;
    }

    public AiFailureLayer getFailureLayer() {
        return failureLayer;
    }

    public AiFailureType getFailureType() {
        return failureType;
    }
}
