package com.zhihang.jobagent.dto;

import com.zhihang.jobagent.entity.JobPost;

import java.util.ArrayList;
import java.util.List;

public class JobMatchResult {

    private JobPost jobPost;

    private Integer matchScore;

    private String compatibilityLevel;

    private String matchReason;

    private String hardConditionSummary;

    private String scoreBreakdown;

    private String weaknessAnalysis;

    private String deductionReasons;

    private String improvementSuggestion;

    private Integer directionScore;

    private Integer skillScore;

    private Integer projectScore;

    private Integer educationScore;

    private Integer cityScore;

    private Integer resumeMaturityScore;

    private Integer dataTrustScore;

    private Integer salarySupportScore;

    private Integer aiRerankScore;

    private Integer compositeRankScore;

    private String preferenceFitLevel;

    private String aiMatchReason;

    private String aiRiskPoints;

    private String recommendedPriority;

    private String suggestedNextAction;

    private String shortTagSummary;

    private String preferenceSummary;

    private final List<String> positiveSignals = new ArrayList<>();

    private final List<String> coreWeaknesses = new ArrayList<>();

    private final List<String> deductionReasonItems = new ArrayList<>();

    private final List<String> nextActionItems = new ArrayList<>();

    public JobMatchResult() {
    }

    public JobMatchResult(JobPost jobPost, Integer matchScore, String matchReason,
                          String weaknessAnalysis, String improvementSuggestion) {
        this.jobPost = jobPost;
        this.matchScore = matchScore;
        this.matchReason = matchReason;
        this.weaknessAnalysis = weaknessAnalysis;
        this.improvementSuggestion = improvementSuggestion;
    }

    public JobPost getJobPost() {
        return jobPost;
    }

    public void setJobPost(JobPost jobPost) {
        this.jobPost = jobPost;
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

    public Integer getDirectionScore() {
        return directionScore;
    }

    public void setDirectionScore(Integer directionScore) {
        this.directionScore = directionScore;
    }

    public Integer getSkillScore() {
        return skillScore;
    }

    public void setSkillScore(Integer skillScore) {
        this.skillScore = skillScore;
    }

    public Integer getProjectScore() {
        return projectScore;
    }

    public void setProjectScore(Integer projectScore) {
        this.projectScore = projectScore;
    }

    public Integer getEducationScore() {
        return educationScore;
    }

    public void setEducationScore(Integer educationScore) {
        this.educationScore = educationScore;
    }

    public Integer getCityScore() {
        return cityScore;
    }

    public void setCityScore(Integer cityScore) {
        this.cityScore = cityScore;
    }

    public Integer getResumeMaturityScore() {
        return resumeMaturityScore;
    }

    public void setResumeMaturityScore(Integer resumeMaturityScore) {
        this.resumeMaturityScore = resumeMaturityScore;
    }

    public Integer getDataTrustScore() {
        return dataTrustScore;
    }

    public void setDataTrustScore(Integer dataTrustScore) {
        this.dataTrustScore = dataTrustScore;
    }

    public Integer getSalarySupportScore() {
        return salarySupportScore;
    }

    public void setSalarySupportScore(Integer salarySupportScore) {
        this.salarySupportScore = salarySupportScore;
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

    public String getRecommendationBadgeTone() {
        return switch (defaultText(recommendedPriority).toUpperCase()) {
            case "HIGH" -> "strong";
            case "MEDIUM" -> "sprint";
            case "LOW" -> "risk";
            default -> switch (defaultText(compatibilityLevel).toUpperCase()) {
                case "STRONG" -> "strong";
                case "WEAK" -> "sprint";
                default -> "risk";
            };
        };
    }

    public String getRecommendationBadgeLabel() {
        return switch (getRecommendationBadgeTone()) {
            case "strong" -> "强推荐";
            case "sprint" -> "可冲刺";
            default -> "风险较高";
        };
    }

    public String getRiskLevelKey() {
        return switch (getRecommendationBadgeTone()) {
            case "strong" -> "low";
            case "sprint" -> "medium";
            default -> "high";
        };
    }

    public String getRiskLevelLabel() {
        return switch (getRiskLevelKey()) {
            case "low" -> "低风险";
            case "medium" -> "中风险";
            default -> "高风险";
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

    public int getPreferenceFitPercent() {
        return switch (defaultText(preferenceFitLevel).toUpperCase()) {
            case "HIGH" -> 92;
            case "MEDIUM" -> 74;
            case "LOW" -> 48;
            default -> 60;
        };
    }

    public int getDisplayCompositeScore() {
        return compositeRankScore != null ? compositeRankScore : (matchScore != null ? matchScore : 0);
    }

    public String getIndustryDisplay() {
        if (jobPost == null) {
            return "-";
        }

        String companyType = defaultText(jobPost.getCompanyTypeDisplay());
        if (!companyType.isEmpty() && !"-".equals(companyType)) {
            return companyType;
        }

        String category = defaultText(jobPost.getDisplayCategory());
        return category.isEmpty() ? "-" : category;
    }

    public boolean isAiEnhanced() {
        return aiRerankScore != null
                || !defaultText(aiMatchReason).isEmpty()
                || !defaultText(recommendedPriority).isEmpty();
    }

    public List<String> getPositiveSignals() {
        return List.copyOf(positiveSignals);
    }

    public void setPositiveSignals(List<String> positiveSignals) {
        resetList(this.positiveSignals, positiveSignals);
    }

    public List<String> getCoreWeaknesses() {
        if (!coreWeaknesses.isEmpty()) {
            return List.copyOf(coreWeaknesses);
        }
        return extractHighlights(weaknessAnalysis, 6);
    }

    public void setCoreWeaknesses(List<String> coreWeaknesses) {
        resetList(this.coreWeaknesses, coreWeaknesses);
    }

    public List<String> getDeductionReasonItems() {
        if (!deductionReasonItems.isEmpty()) {
            return List.copyOf(deductionReasonItems);
        }
        return extractHighlights(deductionReasons, 6);
    }

    public void setDeductionReasonItems(List<String> deductionReasonItems) {
        resetList(this.deductionReasonItems, deductionReasonItems);
    }

    public List<String> getNextActionItems() {
        if (!nextActionItems.isEmpty()) {
            return List.copyOf(nextActionItems);
        }
        return extractHighlights(improvementSuggestion, 6);
    }

    public void setNextActionItems(List<String> nextActionItems) {
        resetList(this.nextActionItems, nextActionItems);
    }

    public List<String> getHardConditionItems() {
        return extractHighlights(hardConditionSummary, 6);
    }

    public List<String> getScoreBreakdownItems() {
        return extractHighlights(scoreBreakdown, 8);
    }

    public List<String> getPrimaryWeaknesses() {
        List<String> weaknesses = getCoreWeaknesses();
        return weaknesses.size() <= 2 ? weaknesses : weaknesses.subList(0, 2);
    }

    public List<String> getAiRiskPointItems() {
        return extractHighlights(aiRiskPoints, 4);
    }

    public List<String> getShortTagSummaryItems() {
        return extractHighlights(shortTagSummary, 4);
    }

    public List<String> getPreferenceSummaryItems() {
        return extractHighlights(preferenceSummary, 6);
    }

    public String getSearchableText() {
        List<String> parts = new ArrayList<>();
        if (jobPost != null) {
            parts.add(jobPost.getDisplayTitle());
            parts.add(jobPost.getCompanyName());
            parts.add(jobPost.getDisplaySummary());
            parts.add(jobPost.getDisplayTags());
            parts.add(jobPost.getJobRequirements());
            parts.add(jobPost.getBonusPoints());
            parts.add(jobPost.getReadableLocationDisplay());
        }
        parts.add(matchReason);
        parts.add(hardConditionSummary);
        parts.add(scoreBreakdown);
        parts.add(weaknessAnalysis);
        parts.add(deductionReasons);
        parts.add(improvementSuggestion);
        parts.add(preferenceSummary);
        parts.add(aiMatchReason);
        parts.add(aiRiskPoints);
        parts.add(shortTagSummary);
        parts.add(suggestedNextAction);

        List<String> normalizedParts = new ArrayList<>();
        for (String part : parts) {
            if (part != null && !part.trim().isEmpty()) {
                normalizedParts.add(part.trim());
            }
        }
        return String.join(" ", normalizedParts);
    }

    private void resetList(List<String> target, List<String> values) {
        target.clear();
        if (values == null) {
            return;
        }
        for (String value : values) {
            String cleaned = defaultText(value);
            if (!cleaned.isEmpty()) {
                target.add(cleaned);
            }
        }
    }

    private List<String> extractHighlights(String text, int limit) {
        List<String> highlights = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return highlights;
        }

        String normalizedText = text.replace('\u3000', ' ')
                .replace("\r", "")
                .replaceAll("[ \\t\\f\\u000B]+", " ")
                .trim();
        String[] segments = normalizedText.split("[。；;\\n]+");
        for (String segment : segments) {
            String value = segment == null ? "" : segment.trim();
            if (value.isEmpty()) {
                continue;
            }
            highlights.add(value);
            if (highlights.size() >= limit) {
                return highlights;
            }
        }

        if (highlights.isEmpty()) {
            highlights.add(normalizedText);
        }
        return highlights;
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }
}
