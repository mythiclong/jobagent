package com.zhihang.jobagent.dto;

import java.util.List;

public class ResumeEnhanceAiResult {

    private final String summary;
    private final List<String> coreProblems;
    private final String matchingOverview;
    private final List<String> missingKeywords;
    private final List<String> improvementSuggestions;
    private final ResumeRewriteBlock educationRewrite;
    private final ResumeRewriteBlock skillsRewrite;
    private final ResumeRewriteBlock projectsRewrite;
    private final ResumeRewriteBlock experienceRewrite;
    private final String fullOptimizedResume;
    private final String generationSource;
    private final String modelName;
    private final String fallbackReason;
    private final boolean aiUsed;
    private final String createdAtDisplay;

    public ResumeEnhanceAiResult(String summary, List<String> coreProblems, String matchingOverview,
                                 List<String> missingKeywords, List<String> improvementSuggestions,
                                 ResumeRewriteBlock educationRewrite, ResumeRewriteBlock skillsRewrite,
                                 ResumeRewriteBlock projectsRewrite, ResumeRewriteBlock experienceRewrite,
                                 String fullOptimizedResume, String generationSource, String modelName,
                                 String fallbackReason, boolean aiUsed, String createdAtDisplay) {
        this.summary = summary;
        this.coreProblems = List.copyOf(coreProblems);
        this.matchingOverview = matchingOverview;
        this.missingKeywords = List.copyOf(missingKeywords);
        this.improvementSuggestions = List.copyOf(improvementSuggestions);
        this.educationRewrite = educationRewrite;
        this.skillsRewrite = skillsRewrite;
        this.projectsRewrite = projectsRewrite;
        this.experienceRewrite = experienceRewrite;
        this.fullOptimizedResume = fullOptimizedResume;
        this.generationSource = generationSource;
        this.modelName = modelName;
        this.fallbackReason = fallbackReason;
        this.aiUsed = aiUsed;
        this.createdAtDisplay = createdAtDisplay;
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

    public List<String> getImprovementSuggestions() {
        return improvementSuggestions;
    }

    public ResumeRewriteBlock getEducationRewrite() {
        return educationRewrite;
    }

    public ResumeRewriteBlock getSkillsRewrite() {
        return skillsRewrite;
    }

    public ResumeRewriteBlock getProjectsRewrite() {
        return projectsRewrite;
    }

    public ResumeRewriteBlock getExperienceRewrite() {
        return experienceRewrite;
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

    public boolean isAiUsed() {
        return aiUsed;
    }

    public String getCreatedAtDisplay() {
        return createdAtDisplay;
    }
}
