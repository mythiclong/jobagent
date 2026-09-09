package com.zhihang.jobagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "job_match_record")
public class JobMatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_account_id")
    private Long userAccountId;

    private Long userProfileId;

    private Long jobPostId;

    private Integer matchScore;

    private String compatibilityLevel;

    @Column(columnDefinition = "TEXT")
    private String matchReason;

    @Column(columnDefinition = "TEXT")
    private String hardConditionSummary;

    @Column(columnDefinition = "TEXT")
    private String scoreBreakdown;

    @Column(columnDefinition = "TEXT")
    private String weaknessAnalysis;

    @Column(columnDefinition = "TEXT")
    private String deductionReasons;

    @Column(columnDefinition = "TEXT")
    private String improvementSuggestion;

    private Integer aiRerankScore;

    private Integer compositeRankScore;

    private String preferenceFitLevel;

    @Column(columnDefinition = "TEXT")
    private String aiMatchReason;

    @Column(columnDefinition = "TEXT")
    private String aiRiskPoints;

    private String recommendedPriority;

    @Column(columnDefinition = "TEXT")
    private String suggestedNextAction;

    @Column(columnDefinition = "TEXT")
    private String shortTagSummary;

    @Column(columnDefinition = "TEXT")
    private String preferenceSummary;

    private LocalDateTime createdAt;

    public JobMatchRecord() {
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

    public Long getJobPostId() {
        return jobPostId;
    }

    public void setJobPostId(Long jobPostId) {
        this.jobPostId = jobPostId;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public String getCompatibilityLevel() {
        return compatibilityLevel;
    }

    public void setCompatibilityLevel(String compatibilityLevel) {
        this.compatibilityLevel = compatibilityLevel;
    }

    public String getCompatibilityLabel() {
        return switch (defaultText(compatibilityLevel).toUpperCase()) {
            case "STRONG" -> "强适配";
            case "WEAK" -> "弱适配";
            case "MISMATCH" -> "明显不适合";
            default -> "待判断";
        };
    }

    public String getMatchReason() {
        return matchReason;
    }

    public void setMatchReason(String matchReason) {
        this.matchReason = matchReason;
    }

    public String getHardConditionSummary() {
        return hardConditionSummary;
    }

    public void setHardConditionSummary(String hardConditionSummary) {
        this.hardConditionSummary = hardConditionSummary;
    }

    public String getScoreBreakdown() {
        return scoreBreakdown;
    }

    public void setScoreBreakdown(String scoreBreakdown) {
        this.scoreBreakdown = scoreBreakdown;
    }

    public String getWeaknessAnalysis() {
        return weaknessAnalysis;
    }

    public void setWeaknessAnalysis(String weaknessAnalysis) {
        this.weaknessAnalysis = weaknessAnalysis;
    }

    public String getDeductionReasons() {
        return deductionReasons;
    }

    public void setDeductionReasons(String deductionReasons) {
        this.deductionReasons = deductionReasons;
    }

    public String getImprovementSuggestion() {
        return improvementSuggestion;
    }

    public void setImprovementSuggestion(String improvementSuggestion) {
        this.improvementSuggestion = improvementSuggestion;
    }

    public Integer getAiRerankScore() {
        return aiRerankScore;
    }

    public void setAiRerankScore(Integer aiRerankScore) {
        this.aiRerankScore = aiRerankScore;
    }

    public Integer getCompositeRankScore() {
        return compositeRankScore;
    }

    public void setCompositeRankScore(Integer compositeRankScore) {
        this.compositeRankScore = compositeRankScore;
    }

    public String getPreferenceFitLevel() {
        return preferenceFitLevel;
    }

    public void setPreferenceFitLevel(String preferenceFitLevel) {
        this.preferenceFitLevel = preferenceFitLevel;
    }

    public String getPreferenceFitLabel() {
        return switch (defaultText(preferenceFitLevel).toUpperCase()) {
            case "HIGH" -> "偏好高度贴合";
            case "MEDIUM" -> "偏好较为贴合";
            case "LOW" -> "偏好一般";
            default -> "偏好待判断";
        };
    }

    public String getAiMatchReason() {
        return aiMatchReason;
    }

    public void setAiMatchReason(String aiMatchReason) {
        this.aiMatchReason = aiMatchReason;
    }

    public String getAiRiskPoints() {
        return aiRiskPoints;
    }

    public void setAiRiskPoints(String aiRiskPoints) {
        this.aiRiskPoints = aiRiskPoints;
    }

    public String getRecommendedPriority() {
        return recommendedPriority;
    }

    public void setRecommendedPriority(String recommendedPriority) {
        this.recommendedPriority = recommendedPriority;
    }

    public String getRecommendedPriorityLabel() {
        return switch (defaultText(recommendedPriority).toUpperCase()) {
            case "HIGH" -> "优先准备";
            case "MEDIUM" -> "可以推进";
            case "LOW" -> "放入观察";
            default -> "规则推荐";
        };
    }

    public String getSuggestedNextAction() {
        return suggestedNextAction;
    }

    public void setSuggestedNextAction(String suggestedNextAction) {
        this.suggestedNextAction = suggestedNextAction;
    }

    public String getShortTagSummary() {
        return shortTagSummary;
    }

    public void setShortTagSummary(String shortTagSummary) {
        this.shortTagSummary = shortTagSummary;
    }

    public String getPreferenceSummary() {
        return preferenceSummary;
    }

    public void setPreferenceSummary(String preferenceSummary) {
        this.preferenceSummary = preferenceSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<String> getMatchReasonItems() {
        return splitStructuredItems(matchReason, 8);
    }

    public List<String> getHardConditionItems() {
        return splitStructuredItems(hardConditionSummary, 6);
    }

    public List<String> getScoreBreakdownItems() {
        return splitStructuredItems(scoreBreakdown, 8);
    }

    public List<String> getWeaknessItems() {
        return splitStructuredItems(weaknessAnalysis, 8);
    }

    public List<String> getDeductionReasonItems() {
        return splitStructuredItems(deductionReasons, 8);
    }

    public List<String> getImprovementItems() {
        return splitStructuredItems(improvementSuggestion, 8);
    }

    public List<String> getAiRiskPointItems() {
        return splitStructuredItems(aiRiskPoints, 6);
    }

    public List<String> getShortTagSummaryItems() {
        return splitStructuredItems(shortTagSummary, 6);
    }

    public List<String> getPreferenceSummaryItems() {
        return splitStructuredItems(preferenceSummary, 6);
    }

    private List<String> splitStructuredItems(String text, int limit) {
        List<String> items = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return items;
        }

        String normalized = text.replace('\u3000', ' ')
                .replace("\r", "")
                .replaceAll("[ \\t\\f\\u000B]+", " ")
                .trim();
        String[] segments = normalized.split("[\\n；;。]+");
        for (String segment : segments) {
            String value = defaultText(segment)
                    .replaceFirst("^[-•]\\s*", "")
                    .replaceFirst("^\\d+[.、)]\\s*", "");
            if (value.isEmpty()) {
                continue;
            }
            items.add(value);
            if (items.size() >= limit) {
                break;
            }
        }
        return items;
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }
}
