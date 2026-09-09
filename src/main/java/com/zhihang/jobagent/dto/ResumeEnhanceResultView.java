package com.zhihang.jobagent.dto;

import java.util.List;

public class ResumeEnhanceResultView {

    private final String summary;
    private final List<String> coreProblems;
    private final String matchingOverview;
    private final List<String> missingKeywords;
    private final List<String> priorityKeywords;
    private final List<String> improvementSuggestions;
    private final String rewrittenEducation;
    private final String rewrittenSkills;
    private final String rewrittenProjects;
    private final String rewrittenExperience;
    private final String fullOptimizedResume;
    private final String generationSource;
    private final String modelName;
    private final String fallbackReason;
    private final String createdAtDisplay;
    private final boolean aiUsed;
    private final List<ResumeEnhanceSectionView> sectionViews;

    public ResumeEnhanceResultView(String summary, List<String> coreProblems, String matchingOverview,
                                   List<String> missingKeywords, List<String> priorityKeywords,
                                   List<String> improvementSuggestions, String rewrittenEducation,
                                   String rewrittenSkills, String rewrittenProjects, String rewrittenExperience,
                                   String fullOptimizedResume, String generationSource, String modelName,
                                   String fallbackReason, String createdAtDisplay, boolean aiUsed,
                                   List<ResumeEnhanceSectionView> sectionViews) {
        this.summary = summary;
        this.coreProblems = List.copyOf(coreProblems);
        this.matchingOverview = matchingOverview;
        this.missingKeywords = List.copyOf(missingKeywords);
        this.priorityKeywords = List.copyOf(priorityKeywords);
        this.improvementSuggestions = List.copyOf(improvementSuggestions);
        this.rewrittenEducation = rewrittenEducation;
        this.rewrittenSkills = rewrittenSkills;
        this.rewrittenProjects = rewrittenProjects;
        this.rewrittenExperience = rewrittenExperience;
        this.fullOptimizedResume = fullOptimizedResume;
        this.generationSource = generationSource;
        this.modelName = modelName;
        this.fallbackReason = fallbackReason;
        this.createdAtDisplay = createdAtDisplay;
        this.aiUsed = aiUsed;
        this.sectionViews = List.copyOf(sectionViews);
    }

    public String getSummary() {
        return summary;
    }

    public List<String> getCoreProblems() {
        return coreProblems;
    }

    public String getMatchingOverview() {
        return matchingOverview;
    }

    public List<String> getMissingKeywords() {
        return missingKeywords;
    }

    public List<String> getPriorityKeywords() {
        return priorityKeywords;
    }

    public List<String> getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public String getRewrittenEducation() {
        return rewrittenEducation;
    }

    public String getRewrittenSkills() {
        return rewrittenSkills;
    }

    public String getRewrittenProjects() {
        return rewrittenProjects;
    }

    public String getRewrittenExperience() {
        return rewrittenExperience;
    }

    public String getFullOptimizedResume() {
        return fullOptimizedResume;
    }

    public String getGenerationSource() {
        return generationSource;
    }

    public String getModelName() {
        return modelName;
    }

    public String getFallbackReason() {
        return fallbackReason;
    }

    public String getCreatedAtDisplay() {
        return createdAtDisplay;
    }

    public boolean isAiUsed() {
        return aiUsed;
    }

    public List<ResumeEnhanceSectionView> getSectionViews() {
        return sectionViews;
    }
}
