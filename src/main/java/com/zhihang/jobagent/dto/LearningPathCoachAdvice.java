package com.zhihang.jobagent.dto;

import java.util.ArrayList;
import java.util.List;

public class LearningPathCoachAdvice {

    private String coachSummary;
    private String impactExplanation;
    private String priorityFocus;
    private String skillOrProjectFirst;
    private String estimatedDuration;
    private String deliveryTimingAdvice;
    private String nextActionSuggestion;
    private final List<String> learningSteps = new ArrayList<>();
    private final List<String> recommendedProjects = new ArrayList<>();
    private final List<String> suggestedOrder = new ArrayList<>();
    private final List<String> resourceTypes = new ArrayList<>();

    public String getCoachSummary() {
        return coachSummary;
    }

    public void setCoachSummary(String coachSummary) {
        this.coachSummary = coachSummary;
    }

    public String getImpactExplanation() {
        return impactExplanation;
    }

    public void setImpactExplanation(String impactExplanation) {
        this.impactExplanation = impactExplanation;
    }

    public String getPriorityFocus() {
        return priorityFocus;
    }

    public void setPriorityFocus(String priorityFocus) {
        this.priorityFocus = priorityFocus;
    }

    public String getSkillOrProjectFirst() {
        return skillOrProjectFirst;
    }

    public void setSkillOrProjectFirst(String skillOrProjectFirst) {
        this.skillOrProjectFirst = skillOrProjectFirst;
    }

    public String getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(String estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

    public String getDeliveryTimingAdvice() {
        return deliveryTimingAdvice;
    }

    public void setDeliveryTimingAdvice(String deliveryTimingAdvice) {
        this.deliveryTimingAdvice = deliveryTimingAdvice;
    }

    public String getNextActionSuggestion() {
        return nextActionSuggestion;
    }

    public void setNextActionSuggestion(String nextActionSuggestion) {
        this.nextActionSuggestion = nextActionSuggestion;
    }

    public List<String> getLearningSteps() {
        return List.copyOf(learningSteps);
    }

    public void setLearningSteps(List<String> learningSteps) {
        resetList(this.learningSteps, learningSteps);
    }

    public List<String> getRecommendedProjects() {
        return List.copyOf(recommendedProjects);
    }

    public void setRecommendedProjects(List<String> recommendedProjects) {
        resetList(this.recommendedProjects, recommendedProjects);
    }

    public List<String> getSuggestedOrder() {
        return List.copyOf(suggestedOrder);
    }

    public void setSuggestedOrder(List<String> suggestedOrder) {
        resetList(this.suggestedOrder, suggestedOrder);
    }

    public List<String> getResourceTypes() {
        return List.copyOf(resourceTypes);
    }

    public void setResourceTypes(List<String> resourceTypes) {
        resetList(this.resourceTypes, resourceTypes);
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
