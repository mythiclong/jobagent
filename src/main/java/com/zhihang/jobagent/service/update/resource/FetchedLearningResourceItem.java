package com.zhihang.jobagent.service.update.resource;

public record FetchedLearningResourceItem(
        String sourceName,
        String title,
        String url,
        String resourceType,
        String targetDirection,
        String stageTag,
        String summary
) {
}
