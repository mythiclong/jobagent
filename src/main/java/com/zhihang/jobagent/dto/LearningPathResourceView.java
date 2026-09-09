package com.zhihang.jobagent.dto;

public class LearningPathResourceView {

    private final String title;
    private final String resourceType;
    private final String link;
    private final String stage;
    private final String summary;
    private final String originalTitle;

    public LearningPathResourceView(String title, String resourceType, String link, String stage) {
        this(title, resourceType, link, stage, "", "");
    }

    public LearningPathResourceView(String title,
                                    String resourceType,
                                    String link,
                                    String stage,
                                    String summary,
                                    String originalTitle) {
        this.title = title;
        this.resourceType = resourceType;
        this.link = link;
        this.stage = stage;
        this.summary = summary;
        this.originalTitle = originalTitle;
    }

    public String getTitle() {
        return title;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getLink() {
        return link;
    }

    public String getStage() {
        return stage;
    }

    public String getSummary() {
        return summary;
    }

    public String getOriginalTitle() {
        return originalTitle;
    }
}
