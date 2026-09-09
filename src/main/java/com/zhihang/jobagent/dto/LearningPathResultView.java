package com.zhihang.jobagent.dto;

import java.util.List;

public class LearningPathResultView {

    private final List<String> profileWeaknesses;
    private final List<String> matchWeaknesses;
    private final List<String> resumeWeaknesses;
    private final List<String> learningSteps;
    private final List<String> recommendedProjects;
    private final List<String> suggestedOrder;
    private final List<String> resourceTypeSuggestions;
    private final List<LearningPathResourceView> recommendedResources;
    private final List<String> deliveryChecklist;
    private final String weaknessSourceSummary;
    private final String preferenceSummary;
    private final String coachSummary;
    private final String impactExplanation;
    private final String priorityFocus;
    private final String skillOrProjectFirst;
    private final String deliveryTimingAdvice;
    private final String nextActionSuggestion;
    private final boolean resumeSignalsMissing;

    public LearningPathResultView(List<String> profileWeaknesses,
                                  List<String> matchWeaknesses,
                                  List<String> resumeWeaknesses,
                                  List<String> learningSteps,
                                  List<String> recommendedProjects,
                                  List<String> suggestedOrder,
                                  List<String> resourceTypeSuggestions,
                                  List<LearningPathResourceView> recommendedResources,
                                  List<String> deliveryChecklist,
                                  String weaknessSourceSummary,
                                  String preferenceSummary,
                                  String coachSummary,
                                  String impactExplanation,
                                  String priorityFocus,
                                  String skillOrProjectFirst,
                                  String deliveryTimingAdvice,
                                  String nextActionSuggestion,
                                  boolean resumeSignalsMissing) {
        this.profileWeaknesses = List.copyOf(profileWeaknesses);
        this.matchWeaknesses = List.copyOf(matchWeaknesses);
        this.resumeWeaknesses = List.copyOf(resumeWeaknesses);
        this.learningSteps = List.copyOf(learningSteps);
        this.recommendedProjects = List.copyOf(recommendedProjects);
        this.suggestedOrder = List.copyOf(suggestedOrder);
        this.resourceTypeSuggestions = List.copyOf(resourceTypeSuggestions);
        this.recommendedResources = List.copyOf(recommendedResources);
        this.deliveryChecklist = List.copyOf(deliveryChecklist);
        this.weaknessSourceSummary = weaknessSourceSummary;
        this.preferenceSummary = preferenceSummary;
        this.coachSummary = coachSummary;
        this.impactExplanation = impactExplanation;
        this.priorityFocus = priorityFocus;
        this.skillOrProjectFirst = skillOrProjectFirst;
        this.deliveryTimingAdvice = deliveryTimingAdvice;
        this.nextActionSuggestion = nextActionSuggestion;
        this.resumeSignalsMissing = resumeSignalsMissing;
    }

    public List<String> getProfileWeaknesses() {
        return profileWeaknesses;
    }

    public List<String> getMatchWeaknesses() {
        return matchWeaknesses;
    }

    public List<String> getResumeWeaknesses() {
        return resumeWeaknesses;
    }

    public List<String> getLearningSteps() {
        return learningSteps;
    }

    public List<String> getRecommendedProjects() {
        return recommendedProjects;
    }

    public List<String> getSuggestedOrder() {
        return suggestedOrder;
    }

    public List<String> getResourceTypeSuggestions() {
        return resourceTypeSuggestions;
    }

    public List<LearningPathResourceView> getRecommendedResources() {
        return recommendedResources;
    }

    public List<String> getDeliveryChecklist() {
        return deliveryChecklist;
    }

    public String getWeaknessSourceSummary() {
        return weaknessSourceSummary;
    }

    public String getPreferenceSummary() {
        return preferenceSummary;
    }

    public String getCoachSummary() {
        return coachSummary;
    }

    public String getImpactExplanation() {
        return impactExplanation;
    }

    public String getPriorityFocus() {
        return priorityFocus;
    }

    public String getSkillOrProjectFirst() {
        return skillOrProjectFirst;
    }

    public String getDeliveryTimingAdvice() {
        return deliveryTimingAdvice;
    }

    public String getNextActionSuggestion() {
        return nextActionSuggestion;
    }

    public boolean isResumeSignalsMissing() {
        return resumeSignalsMissing;
    }
}
