package com.zhihang.jobagent.dto;

import java.time.LocalDateTime;

public class InterviewSessionSummary {

    private String sessionId;

    private Long userProfileId;

    private Long jobPostId;

    private Integer totalScore;

    private Integer questionCount;

    private LocalDateTime createdAt;

    public InterviewSessionSummary() {
    }

    public InterviewSessionSummary(String sessionId, Long userProfileId, Long jobPostId,
                                   Integer totalScore, Integer questionCount, LocalDateTime createdAt) {
        this.sessionId = sessionId;
        this.userProfileId = userProfileId;
        this.jobPostId = jobPostId;
        this.totalScore = totalScore;
        this.questionCount = questionCount;
        this.createdAt = createdAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public Integer getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore) {
        this.totalScore = totalScore;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
