package com.zhihang.jobagent.dto;

import java.util.List;

public class ResumeProfileSyncSuggestion {

    private final boolean draftMode;
    private final String profileDisplayName;
    private final String recognizedTargetRole;
    private final String recognizedDirection;
    private final String recognizedTargetCity;
    private final String recognizedJobNature;
    private final String recognizedIndustry;
    private final String recognizedExpectedSalary;
    private final String recognizedCareerGoal;
    private final String skillSummary;
    private final String projectSummary;
    private final List<ProfileSyncFieldSuggestion> fieldSuggestions;
    private final int recommendedCount;
    private final boolean hasRecommendedUpdates;

    public ResumeProfileSyncSuggestion(boolean draftMode,
                                       String profileDisplayName,
                                       String recognizedTargetRole,
                                       String recognizedDirection,
                                       String recognizedTargetCity,
                                       String recognizedJobNature,
                                       String recognizedIndustry,
                                       String recognizedExpectedSalary,
                                       String recognizedCareerGoal,
                                       String skillSummary,
                                       String projectSummary,
                                       List<ProfileSyncFieldSuggestion> fieldSuggestions,
                                       int recommendedCount,
                                       boolean hasRecommendedUpdates) {
        this.draftMode = draftMode;
        this.profileDisplayName = profileDisplayName;
        this.recognizedTargetRole = recognizedTargetRole;
        this.recognizedDirection = recognizedDirection;
        this.recognizedTargetCity = recognizedTargetCity;
        this.recognizedJobNature = recognizedJobNature;
        this.recognizedIndustry = recognizedIndustry;
        this.recognizedExpectedSalary = recognizedExpectedSalary;
        this.recognizedCareerGoal = recognizedCareerGoal;
        this.skillSummary = skillSummary;
        this.projectSummary = projectSummary;
        this.fieldSuggestions = List.copyOf(fieldSuggestions);
        this.recommendedCount = recommendedCount;
        this.hasRecommendedUpdates = hasRecommendedUpdates;
    }

    public boolean isDraftMode() {
        return draftMode;
    }

    public String getProfileDisplayName() {
        return profileDisplayName;
    }

    public String getRecognizedTargetRole() {
        return recognizedTargetRole;
    }

    public String getRecognizedDirection() {
        return recognizedDirection;
    }

    public String getDirectionCandidateDisplay() {
        return recognizedDirection;
    }

    public String getRecognizedTargetCity() {
        return recognizedTargetCity;
    }

    public String getRecognizedJobNature() {
        return recognizedJobNature;
    }

    public String getRecognizedIndustry() {
        return recognizedIndustry;
    }

    public String getRecognizedExpectedSalary() {
        return recognizedExpectedSalary;
    }

    public String getRecognizedCareerGoal() {
        return recognizedCareerGoal;
    }

    public String getCareerGoalCandidateDisplay() {
        return recognizedCareerGoal;
    }

    public String getSkillSummary() {
        return skillSummary;
    }

    public String getProjectSummary() {
        return projectSummary;
    }

    public List<ProfileSyncFieldSuggestion> getFieldSuggestions() {
        return fieldSuggestions;
    }

    public int getRecommendedCount() {
        return recommendedCount;
    }

    public boolean isHasRecommendedUpdates() {
        return hasRecommendedUpdates;
    }

    public boolean hasRecommendedUpdates() {
        return hasRecommendedUpdates;
    }
}
