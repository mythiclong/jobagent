package com.zhihang.jobagent.dto;

public class ProfileSyncFieldSuggestion {

    private final String fieldKey;
    private final String label;
    private final String currentValue;
    private final String recognizedValue;
    private final String suggestionAction;
    private final boolean recommended;

    public ProfileSyncFieldSuggestion(String fieldKey, String label, String currentValue, String recognizedValue,
                                      String suggestionAction, boolean recommended) {
        this.fieldKey = fieldKey;
        this.label = label;
        this.currentValue = currentValue;
        this.recognizedValue = recognizedValue;
        this.suggestionAction = suggestionAction;
        this.recommended = recommended;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public String getLabel() {
        return label;
    }

    public String getCurrentValue() {
        return currentValue;
    }

    public String getCurrentValueDisplay() {
        return currentValue;
    }

    public String getRecognizedValue() {
        return recognizedValue;
    }

    public String getExtractedValueDisplay() {
        return recognizedValue;
    }

    public String getSuggestionAction() {
        return suggestionAction;
    }

    public String getActionLabel() {
        return suggestionAction;
    }

    public boolean isRecommended() {
        return recommended;
    }
}
