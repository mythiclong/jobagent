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
@Table(name = "learning_resource")
public class LearningResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String resourceType;

    private String targetDirection;

    private String stageTag;

    @Column(nullable = false)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String source;

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

    private LocalDateTime updatedAt;

    public LearningResource() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getTargetDirection() {
        return targetDirection;
    }

    public void setTargetDirection(String targetDirection) {
        this.targetDirection = targetDirection;
    }

    public String getStageTag() {
        return stageTag;
    }

    public void setStageTag(String stageTag) {
        this.stageTag = stageTag;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getRawTitle() {
        return StringUtils.hasText(rawTitle) ? rawTitle : title;
    }

    public void setRawTitle(String rawTitle) {
        this.rawTitle = rawTitle;
    }

    public String getRawContent() {
        return StringUtils.hasText(rawContent) ? rawContent : summary;
    }

    public void setRawContent(String rawContent) {
        this.rawContent = rawContent;
    }

    public String getRawUrl() {
        return StringUtils.hasText(rawUrl) ? rawUrl : url;
    }

    public void setRawUrl(String rawUrl) {
        this.rawUrl = rawUrl;
    }

    public String getSourceLanguage() {
        return StringUtils.hasText(sourceLanguage) ? sourceLanguage : (containsChinese(getRawTitle() + getRawContent()) ? "中文" : "未知");
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    public String getDisplayTitle() {
        return StringUtils.hasText(displayTitle) ? displayTitle : title;
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    public String getDisplaySummary() {
        return StringUtils.hasText(displaySummary) ? displaySummary : summary;
    }

    public void setDisplaySummary(String displaySummary) {
        this.displaySummary = displaySummary;
    }

    public String getDisplayTags() {
        return StringUtils.hasText(displayTags) ? displayTags : targetDirection;
    }

    public void setDisplayTags(String displayTags) {
        this.displayTags = displayTags;
    }

    public String getDisplayCategory() {
        return StringUtils.hasText(displayCategory) ? displayCategory : resourceType;
    }

    public void setDisplayCategory(String displayCategory) {
        this.displayCategory = displayCategory;
    }

    public String getDisplayLanguage() {
        return StringUtils.hasText(displayLanguage) ? displayLanguage : "中文";
    }

    public void setDisplayLanguage(String displayLanguage) {
        this.displayLanguage = displayLanguage;
    }

    public String getDisplayDifficulty() {
        return StringUtils.hasText(displayDifficulty) ? displayDifficulty : "中等";
    }

    public void setDisplayDifficulty(String displayDifficulty) {
        this.displayDifficulty = displayDifficulty;
    }

    public String getDisplayStage() {
        return StringUtils.hasText(displayStage) ? displayStage : stageTag;
    }

    public void setDisplayStage(String displayStage) {
        this.displayStage = displayStage;
    }

    public String getNormalizedStatus() {
        return StringUtils.hasText(normalizedStatus) ? normalizedStatus : "待整理";
    }

    public void setNormalizedStatus(String normalizedStatus) {
        this.normalizedStatus = normalizedStatus;
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
        return splitItems(getDisplayTags(), "/", "、", ",", "，");
    }

    private List<String> splitItems(String source, String... separators) {
        String working = source == null ? "" : source;
        for (String separator : separators) {
            working = working.replace(separator, "\n");
        }
        String[] parts = working.split("\\n");
        Set<String> result = new LinkedHashSet<>();
        for (String part : parts) {
            String item = part == null ? "" : part.trim();
            if (!item.isEmpty()) {
                result.add(item);
            }
        }
        return new ArrayList<>(result);
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
