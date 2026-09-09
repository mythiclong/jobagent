package com.zhihang.jobagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "learning_path_advice")
public class LearningPathAdvice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_account_id")
    private Long userAccountId;

    private Long userProfileId;

    private Long targetJobId;

    private String targetDirection;

    @Column(columnDefinition = "TEXT")
    private String currentWeaknesses;

    @Column(columnDefinition = "TEXT")
    private String learningSteps;

    private String learningStepsGenerationSource;

    private Boolean learningStepsAiUsed;

    @Column(columnDefinition = "TEXT")
    private String learningStepsFallbackReason;

    private String learningStepsModelName;

    @Column(columnDefinition = "TEXT")
    private String recommendedProjects;

    @Column(columnDefinition = "TEXT")
    private String suggestedOrder;

    @Column(columnDefinition = "TEXT")
    private String recommendedResources;

    @Column(columnDefinition = "TEXT")
    private String coachSummary;

    @Column(columnDefinition = "TEXT")
    private String impactExplanation;

    @Column(columnDefinition = "TEXT")
    private String priorityFocus;

    private String skillOrProjectFirst;

    @Column(columnDefinition = "TEXT")
    private String deliveryTimingAdvice;

    @Column(columnDefinition = "TEXT")
    private String nextActionSuggestion;

    @Column(columnDefinition = "TEXT")
    private String coachResourceTypes;

    @Column(columnDefinition = "TEXT")
    private String preferenceSummary;

    private String coachGenerationSource;

    private Boolean coachAiUsed;

    @Column(columnDefinition = "TEXT")
    private String coachFallbackReason;

    private String coachModelName;

    private String estimatedDuration;

    private String suggestedLevel;

    private String deliveryReadiness;

    private LocalDateTime createdAt;

    public LearningPathAdvice() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserAccountId() {
        return userAccountId;
    }

    public void setUserAccountId(Long userAccountId) {
        this.userAccountId = userAccountId;
    }

    public Long getUserProfileId() {
        return userProfileId;
    }

    public void setUserProfileId(Long userProfileId) {
        this.userProfileId = userProfileId;
    }

    public Long getTargetJobId() {
        return targetJobId;
    }

    public void setTargetJobId(Long targetJobId) {
        this.targetJobId = targetJobId;
    }

    public String getTargetDirection() {
        return targetDirection;
    }

    public void setTargetDirection(String targetDirection) {
        this.targetDirection = targetDirection;
    }

    public String getCurrentWeaknesses() {
        return currentWeaknesses;
    }

    public void setCurrentWeaknesses(String currentWeaknesses) {
        this.currentWeaknesses = currentWeaknesses;
    }

    public String getLearningSteps() {
        return learningSteps;
    }

    public void setLearningSteps(String learningSteps) {
        this.learningSteps = learningSteps;
    }

    public String getLearningStepsGenerationSource() {
        return learningStepsGenerationSource;
    }

    public void setLearningStepsGenerationSource(String learningStepsGenerationSource) {
        this.learningStepsGenerationSource = learningStepsGenerationSource;
    }

    public Boolean getLearningStepsAiUsed() {
        return learningStepsAiUsed;
    }

    public void setLearningStepsAiUsed(Boolean learningStepsAiUsed) {
        this.learningStepsAiUsed = learningStepsAiUsed;
    }

    public String getLearningStepsFallbackReason() {
        return learningStepsFallbackReason;
    }

    public void setLearningStepsFallbackReason(String learningStepsFallbackReason) {
        this.learningStepsFallbackReason = learningStepsFallbackReason;
    }

    public String getLearningStepsModelName() {
        return learningStepsModelName;
    }

    public void setLearningStepsModelName(String learningStepsModelName) {
        this.learningStepsModelName = learningStepsModelName;
    }

    public String getRecommendedProjects() {
        return recommendedProjects;
    }

    public void setRecommendedProjects(String recommendedProjects) {
        this.recommendedProjects = recommendedProjects;
    }

    public String getSuggestedOrder() {
        return suggestedOrder;
    }

    public void setSuggestedOrder(String suggestedOrder) {
        this.suggestedOrder = suggestedOrder;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getRecommendedResources() {
        return recommendedResources;
    }

    public void setRecommendedResources(String recommendedResources) {
        this.recommendedResources = recommendedResources;
    }

    public String getEstimatedDuration() {
        return estimatedDuration;
    }

    public void setEstimatedDuration(String estimatedDuration) {
        this.estimatedDuration = estimatedDuration;
    }

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

    public String getCoachResourceTypes() {
        return coachResourceTypes;
    }

    public void setCoachResourceTypes(String coachResourceTypes) {
        this.coachResourceTypes = coachResourceTypes;
    }

    public String getPreferenceSummary() {
        return preferenceSummary;
    }

    public void setPreferenceSummary(String preferenceSummary) {
        this.preferenceSummary = preferenceSummary;
    }

    public String getCoachGenerationSource() {
        return coachGenerationSource;
    }

    public void setCoachGenerationSource(String coachGenerationSource) {
        this.coachGenerationSource = coachGenerationSource;
    }

    public Boolean getCoachAiUsed() {
        return coachAiUsed;
    }

    public void setCoachAiUsed(Boolean coachAiUsed) {
        this.coachAiUsed = coachAiUsed;
    }

    public String getCoachFallbackReason() {
        return coachFallbackReason;
    }

    public void setCoachFallbackReason(String coachFallbackReason) {
        this.coachFallbackReason = coachFallbackReason;
    }

    public String getCoachModelName() {
        return coachModelName;
    }

    public void setCoachModelName(String coachModelName) {
        this.coachModelName = coachModelName;
    }

    public String getSuggestedLevel() {
        return suggestedLevel;
    }

    public void setSuggestedLevel(String suggestedLevel) {
        this.suggestedLevel = suggestedLevel;
    }

    public String getDeliveryReadiness() {
        return deliveryReadiness;
    }

    public void setDeliveryReadiness(String deliveryReadiness) {
        this.deliveryReadiness = deliveryReadiness;
    }
}
