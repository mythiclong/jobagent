package com.zhihang.jobagent.dto;

public class ResumeAiReviewResult {

    private final String summary;
    private final String problems;
    private final String missingKeywords;
    private final String suggestions;

    public ResumeAiReviewResult(String summary, String problems, String missingKeywords, String suggestions) {
        this.summary = summary;
        this.problems = problems;
        this.missingKeywords = missingKeywords;
        this.suggestions = suggestions;
    }

    public String getSummary() {
        return summary;
    }

    public String getProblems() {
        return problems;
    }

    public String getMissingKeywords() {
        return missingKeywords;
    }

    public String getSuggestions() {
        return suggestions;
    }
}
