package com.zhihang.jobagent.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class JobMatchAiEnhancementResult {

    private final Map<Long, JobMatchAiInsight> insightByJobPostId;
    private final String summary;
    private final AiGenerationMetadata metadata;

    public JobMatchAiEnhancementResult(Map<Long, JobMatchAiInsight> insightByJobPostId,
                                       String summary,
                                       AiGenerationMetadata metadata) {
        this.insightByJobPostId = insightByJobPostId == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(insightByJobPostId));
        this.summary = summary;
        this.metadata = metadata;
    }

    public Map<Long, JobMatchAiInsight> getInsightByJobPostId() {
        return insightByJobPostId;
    }

    public String getSummary() {
        return summary;
    }

    public AiGenerationMetadata getMetadata() {
        return metadata;
    }
}
