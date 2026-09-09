package com.zhihang.jobagent.dto;

public class ResumeEnhanceSectionView {

    private final String sectionKey;
    private final String sectionTitle;
    private final String originalText;
    private final String rewrittenText;
    private final String rewriteReason;
    private final String generationSource;
    private final String modelName;
    private final String fallbackReason;
    private final boolean rewritten;

    public ResumeEnhanceSectionView(String sectionKey, String sectionTitle, String originalText, String rewrittenText,
                                    String rewriteReason, String generationSource, String modelName,
                                    String fallbackReason, boolean rewritten) {
        this.sectionKey = sectionKey;
        this.sectionTitle = sectionTitle;
        this.originalText = originalText;
        this.rewrittenText = rewrittenText;
        this.rewriteReason = rewriteReason;
        this.generationSource = generationSource;
        this.modelName = modelName;
        this.fallbackReason = fallbackReason;
        this.rewritten = rewritten;
    }

    public String getSectionKey() {
        return sectionKey;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getRewrittenText() {
        return rewrittenText;
    }

    public String getRewriteReason() {
        return rewriteReason;
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

    public boolean isRewritten() {
        return rewritten;
    }
}
