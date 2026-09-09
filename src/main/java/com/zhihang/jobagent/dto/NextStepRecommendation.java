package com.zhihang.jobagent.dto;

public class NextStepRecommendation {

    private final String key;
    private final String title;
    private final String description;
    private final String buttonLabel;
    private final String href;

    public NextStepRecommendation(String key, String title, String description, String buttonLabel, String href) {
        this.key = key;
        this.title = title;
        this.description = description;
        this.buttonLabel = buttonLabel;
        this.href = href;
    }

    public String getKey() {
        return key;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getButtonLabel() {
        return buttonLabel;
    }

    public String getHref() {
        return href;
    }
}
