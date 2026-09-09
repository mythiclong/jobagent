package com.zhihang.jobagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "interview_question")
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String jobDirection;

    private String questionType;

    @Column(columnDefinition = "TEXT")
    private String questionText;

    @Column(columnDefinition = "TEXT")
    private String referenceAnswer;

    @Column(columnDefinition = "TEXT")
    private String keyPoints;

    private String sourceName;

    private String sourceType;

    private String providerName;

    private String roleCategory;

    private String sourceUrl;

    private String questionHash;

    @Column(columnDefinition = "TEXT")
    private String sourceSummary;

    @Column(columnDefinition = "TEXT")
    private String scoringRubric;

    private String rawTitle;

    @Column(columnDefinition = "TEXT")
    private String rawContent;

    private String rawUrl;

    private String sourceLanguage;

    private String displayTitle;

    @Column(columnDefinition = "TEXT")
    private String displaySummary;

    private String displayTags;

    private String displayCategory;

    private String displayLanguage;

    private String displayDifficulty;

    private String displayStage;

    private String normalizedStatus;

    private LocalDateTime generatedAt;

    private LocalDateTime updatedAt;

    public InterviewQuestion() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobDirection() {
        return jobDirection;
    }

    public void setJobDirection(String jobDirection) {
        this.jobDirection = jobDirection;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getReferenceAnswer() {
        return referenceAnswer;
    }

    public void setReferenceAnswer(String referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
    }

    public String getKeyPoints() {
        return keyPoints;
    }

    public void setKeyPoints(String keyPoints) {
        this.keyPoints = keyPoints;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getSourceType() {
        if (StringUtils.hasText(sourceType)) {
            return sourceType.trim();
        }
        return StringUtils.hasText(providerName) ? "AI_GENERATED" : "CURATED";
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getRoleCategory() {
        return StringUtils.hasText(roleCategory) ? roleCategory.trim() : getJobDirection();
    }

    public void setRoleCategory(String roleCategory) {
        this.roleCategory = roleCategory;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public String getQuestionHash() {
        return questionHash;
    }

    public void setQuestionHash(String questionHash) {
        this.questionHash = questionHash;
    }

    public String getSourceSummary() {
        return sourceSummary;
    }

    public void setSourceSummary(String sourceSummary) {
        this.sourceSummary = sourceSummary;
    }

    public String getScoringRubric() {
        return scoringRubric;
    }

    public void setScoringRubric(String scoringRubric) {
        this.scoringRubric = scoringRubric;
    }

    public String getRawTitle() {
        return firstNonBlank(rawTitle, questionText);
    }

    public void setRawTitle(String rawTitle) {
        this.rawTitle = rawTitle;
    }

    public String getRawContent() {
        return firstNonBlank(rawContent, joinNonBlank(sourceSummary, referenceAnswer, keyPoints, questionText));
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public String getRawUrl() {
        return firstNonBlank(rawUrl, sourceUrl);
    }

    public void setRawUrl(String rawUrl) {
        this.rawUrl = rawUrl;
    }

    public String getSourceLanguage() {
        if (StringUtils.hasText(sourceLanguage)) {
            return sourceLanguage.trim();
        }
        return containsChinese(getRawTitle() + getRawContent()) ? "中文" : "未知";
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String getDisplayTitle() {
        return StringUtils.hasText(displayTitle) ? displayTitle.trim() : firstNonBlank(questionText, rawTitle, "面试题");
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    public String getDisplaySummary() {
        return StringUtils.hasText(displaySummary) ? displaySummary.trim() : defaultText(sourceSummary);
    }

    public void setDisplaySummary(String displaySummary) {
        this.displaySummary = displaySummary;
    }

    public String getDisplayTags() {
        return StringUtils.hasText(displayTags) ? displayTags.trim() : firstNonBlank(roleCategory, jobDirection);
    }

    public void setDisplayTags(String displayTags) {
        this.displayTags = displayTags;
    }

    public String getDisplayCategory() {
        return StringUtils.hasText(displayCategory) ? displayCategory.trim() : firstNonBlank(questionType, "基础知识");
    }

    public void setDisplayCategory(String displayCategory) {
        this.displayCategory = displayCategory;
    }

    public String getDisplayLanguage() {
        return StringUtils.hasText(displayLanguage) ? displayLanguage.trim() : "中文";
    }

    public void setDisplayLanguage(String displayLanguage) {
        this.displayLanguage = displayLanguage;
    }

    public String getDisplayDifficulty() {
        return StringUtils.hasText(displayDifficulty) ? displayDifficulty.trim() : "基础";
    }

    public void setDisplayDifficulty(String displayDifficulty) {
        this.displayDifficulty = displayDifficulty;
    }

    public String getDisplayStage() {
        return StringUtils.hasText(displayStage) ? displayStage.trim() : "面试准备";
    }

    public void setDisplayStage(String displayStage) {
        this.displayStage = displayStage;
    }

    public String getNormalizedStatus() {
        return StringUtils.hasText(normalizedStatus) ? normalizedStatus.trim() : "待整理";
    }

    public void setNormalizedStatus(String normalizedStatus) {
        this.normalizedStatus = normalizedStatus;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isOrganized() {
        return !"待整理".equals(getNormalizedStatus());
    }

    public List<String> getDisplayTagList() {
        return splitItems(getDisplayTags(), "/", "、", ",", "，", ";", "；", "\n");
    }

    public List<String> getKeyPointItemList() {
        return splitItems(keyPoints, "\n", "；", ";", "。", ",", "，");
    }

    public String getKeyPointSummary() {
        List<String> items = getKeyPointItemList();
        return items.isEmpty() ? "" : String.join(" / ", items);
    }

    public List<String> getScoringRubricItems() {
        return splitItems(scoringRubric, "\n", "；", ";", "。", ",", "，");
    }

    private List<String> splitItems(String source, String... separators) {
        String working = defaultText(source);
        for (String separator : separators) {
            working = working.replace(separator, "\n");
        }
        String[] parts = working.split("\\n");
        Set<String> result = new LinkedHashSet<>();
        for (String part : parts) {
            String item = defaultText(part);
            if (!item.isEmpty()) {
                result.add(item);
            }
        }
        return new ArrayList<>(result);
    }

    private String joinNonBlank(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            String cleaned = defaultText(value);
            if (!cleaned.isEmpty()) {
                parts.add(cleaned);
            }
        }
        return String.join("\n", parts);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String cleaned = defaultText(value);
            if (!cleaned.isEmpty()) {
                return cleaned;
            }
        }
        return "";
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }

    private boolean containsChinese(String source) {
        if (!StringUtils.hasText(source)) {
            return false;
        }
        for (char ch : source.toCharArray()) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(ch);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B) {
                return true;
            }
        }
        return false;
    }
}
