package com.zhihang.jobagent.dto;

import java.util.ArrayList;
import java.util.List;

public class JobMatchAiInsight {

    private Long jobPostId;
    private Integer rerankScore;
    private String preferenceFitLevel;
    private String aiMatchReason;
    private String suggestedNextAction;
    private String recommendedPriority;
    private final List<String> aiRiskPoints = new ArrayList<>();
    private final List<String> shortTagSummary = new ArrayList<>();

    public Long getJobPostId() {
        return jobPostId;
    }

    public void setJobPostId(Long jobPostId) {
        this.jobPostId = jobPostId;
    }

    public Integer getRerankScore() {
        return rerankScore;
    }

    public void setRerankScore(Integer rerankScore) {
        this.rerankScore = rerankScore;
    }

    public String getPreferenceFitLevel() {
        return preferenceFitLevel;
    }

    public void setPreferenceFitLevel(String preferenceFitLevel) {
        this.preferenceFitLevel = preferenceFitLevel;
    }

    public String getAiMatchReason() {
        return aiMatchReason;
    }

    public void setAiMatchReason(String aiMatchReason) {
        this.aiMatchReason = aiMatchReason;
    }

    public String getSuggestedNextAction() {
        return suggestedNextAction;
    }

    public void setSuggestedNextAction(String suggestedNextAction) {
        this.suggestedNextAction = suggestedNextAction;
    }

    public String getRecommendedPriority() {
        return recommendedPriority;
    }

    public void setRecommendedPriority(String recommendedPriority) {
        this.recommendedPriority = recommendedPriority;
    }

    public List<String> getAiRiskPoints() {
        return List.copyOf(aiRiskPoints);
    }

    public void setAiRiskPoints(List<String> aiRiskPoints) {
        resetList(this.aiRiskPoints, aiRiskPoints);
    }

    public List<String> getShortTagSummary() {
        return List.copyOf(shortTagSummary);
    }

    public void setShortTagSummary(List<String> shortTagSummary) {
        resetList(this.shortTagSummary, shortTagSummary);
    }

    private void resetList(List<String> target, List<String> source) {
        target.clear();
        if (source == null) {
            return;
        }
        for (String item : source) {
            if (item != null && !item.trim().isEmpty()) {
                target.add(item.trim());
            }
        }
    }
}
