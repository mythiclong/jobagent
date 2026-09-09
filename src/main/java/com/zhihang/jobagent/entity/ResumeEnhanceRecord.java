package com.zhihang.jobagent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "resume_enhance_record")
public class ResumeEnhanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_account_id")
    private Long userAccountId;

    private Long userProfileId;

    private Long jobPostId;

    private String inputType;

    @Column(columnDefinition = "TEXT")
    private String originalResumeText;

    @Column(columnDefinition = "TEXT")
    private String parsedBasicInfo;

    @Column(columnDefinition = "TEXT")
    private String parsedEducation;

    @Column(columnDefinition = "TEXT")
    private String parsedSkills;

    @Column(columnDefinition = "TEXT")
    private String parsedProjects;

    @Column(columnDefinition = "TEXT")
    private String parsedExperience;

    @Column(columnDefinition = "TEXT")
    private String parsedOthers;

    private Integer reviewScore;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Column(columnDefinition = "TEXT")
    private String aiProblems;

    @Column(columnDefinition = "TEXT")
    private String aiMissingKeywords;

    @Column(columnDefinition = "TEXT")
    private String aiSuggestions;

    private String reviewGenerationSource;

    private Boolean reviewAiUsed;

    @Column(columnDefinition = "TEXT")
    private String reviewFallbackReason;

    private String reviewModelName;

    @Column(columnDefinition = "TEXT")
    private String optimizedBasicInfo;

    private String basicInfoGenerationSource;

    @Column(columnDefinition = "TEXT")
    private String basicInfoFallbackReason;

    private String basicInfoModelName;

    @Column(columnDefinition = "TEXT")
    private String optimizedEducation;

    private String educationGenerationSource;

    @Column(columnDefinition = "TEXT")
    private String educationFallbackReason;

    private String educationModelName;

    @Column(columnDefinition = "TEXT")
    private String optimizedSkills;

    private String skillsGenerationSource;

    @Column(columnDefinition = "TEXT")
    private String skillsFallbackReason;

    private String skillsModelName;

    @Column(columnDefinition = "TEXT")
    private String optimizedProjects;

    private String projectsGenerationSource;

    @Column(columnDefinition = "TEXT")
    private String projectsFallbackReason;

    private String projectsModelName;

    @Column(columnDefinition = "TEXT")
    private String optimizedExperience;

    private String experienceGenerationSource;

    @Column(columnDefinition = "TEXT")
    private String experienceFallbackReason;

    private String experienceModelName;

    @Column(columnDefinition = "TEXT")
    private String optimizedOthers;

    private String othersGenerationSource;

    @Column(columnDefinition = "TEXT")
    private String othersFallbackReason;

    private String othersModelName;

    @Column(columnDefinition = "TEXT")
    private String fullOptimizedResume;

    private String fullResumeGenerationSource;

    @Column(columnDefinition = "TEXT")
    private String fullResumeFallbackReason;

    private String fullResumeModelName;

    private LocalDateTime createdAt;

    public ResumeEnhanceRecord() {
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

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getOriginalResumeText() {
        return originalResumeText;
    }

    public void setOriginalResumeText(String originalResumeText) {
        this.originalResumeText = originalResumeText;
    }

    public String getParsedBasicInfo() {
        return parsedBasicInfo;
    }

    public void setParsedBasicInfo(String parsedBasicInfo) {
        this.parsedBasicInfo = parsedBasicInfo;
    }

    public String getParsedEducation() {
        return parsedEducation;
    }

    public void setParsedEducation(String parsedEducation) {
        this.parsedEducation = parsedEducation;
    }

    public String getParsedSkills() {
        return parsedSkills;
    }

    public void setParsedSkills(String parsedSkills) {
        this.parsedSkills = parsedSkills;
    }

    public String getParsedProjects() {
        return parsedProjects;
    }

    public void setParsedProjects(String parsedProjects) {
        this.parsedProjects = parsedProjects;
    }

    public String getParsedExperience() {
        return parsedExperience;
    }

    public void setParsedExperience(String parsedExperience) {
        this.parsedExperience = parsedExperience;
    }

    public String getParsedOthers() {
        return parsedOthers;
    }

    public void setParsedOthers(String parsedOthers) {
        this.parsedOthers = parsedOthers;
    }

    public Integer getReviewScore() {
        return reviewScore;
    }

    public void setReviewScore(Integer reviewScore) {
        this.reviewScore = reviewScore;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getAiProblems() {
        return aiProblems;
    }

    public void setAiProblems(String aiProblems) {
        this.aiProblems = aiProblems;
    }

    public String getAiMissingKeywords() {
        return aiMissingKeywords;
    }

    public void setAiMissingKeywords(String aiMissingKeywords) {
        this.aiMissingKeywords = aiMissingKeywords;
    }

    public String getAiSuggestions() {
        return aiSuggestions;
    }

    public void setAiSuggestions(String aiSuggestions) {
        this.aiSuggestions = aiSuggestions;
    }

    public String getReviewGenerationSource() {
        return reviewGenerationSource;
    }

    public void setReviewGenerationSource(String reviewGenerationSource) {
        this.reviewGenerationSource = reviewGenerationSource;
    }

    public Boolean getReviewAiUsed() {
        return reviewAiUsed;
    }

    public void setReviewAiUsed(Boolean reviewAiUsed) {
        this.reviewAiUsed = reviewAiUsed;
    }

    public String getReviewFallbackReason() {
        return reviewFallbackReason;
    }

    public void setReviewFallbackReason(String reviewFallbackReason) {
        this.reviewFallbackReason = reviewFallbackReason;
    }

    public String getReviewModelName() {
        return reviewModelName;
    }

    public void setReviewModelName(String reviewModelName) {
        this.reviewModelName = reviewModelName;
    }

    public String getOptimizedBasicInfo() {
        return optimizedBasicInfo;
    }

    public void setOptimizedBasicInfo(String optimizedBasicInfo) {
        this.optimizedBasicInfo = optimizedBasicInfo;
    }

    public String getBasicInfoGenerationSource() {
        return basicInfoGenerationSource;
    }

    public void setBasicInfoGenerationSource(String basicInfoGenerationSource) {
        this.basicInfoGenerationSource = basicInfoGenerationSource;
    }

    public String getBasicInfoFallbackReason() {
        return basicInfoFallbackReason;
    }

    public void setBasicInfoFallbackReason(String basicInfoFallbackReason) {
        this.basicInfoFallbackReason = basicInfoFallbackReason;
    }

    public String getBasicInfoModelName() {
        return basicInfoModelName;
    }

    public void setBasicInfoModelName(String basicInfoModelName) {
        this.basicInfoModelName = basicInfoModelName;
    }

    public String getOptimizedEducation() {
        return optimizedEducation;
    }

    public void setOptimizedEducation(String optimizedEducation) {
        this.optimizedEducation = optimizedEducation;
    }

    public String getEducationGenerationSource() {
        return educationGenerationSource;
    }

    public void setEducationGenerationSource(String educationGenerationSource) {
        this.educationGenerationSource = educationGenerationSource;
    }

    public String getEducationFallbackReason() {
        return educationFallbackReason;
    }

    public void setEducationFallbackReason(String educationFallbackReason) {
        this.educationFallbackReason = educationFallbackReason;
    }

    public String getEducationModelName() {
        return educationModelName;
    }

    public void setEducationModelName(String educationModelName) {
        this.educationModelName = educationModelName;
    }

    public String getOptimizedSkills() {
        return optimizedSkills;
    }

    public void setOptimizedSkills(String optimizedSkills) {
        this.optimizedSkills = optimizedSkills;
    }

    public String getSkillsGenerationSource() {
        return skillsGenerationSource;
    }

    public void setSkillsGenerationSource(String skillsGenerationSource) {
        this.skillsGenerationSource = skillsGenerationSource;
    }

    public String getSkillsFallbackReason() {
        return skillsFallbackReason;
    }

    public void setSkillsFallbackReason(String skillsFallbackReason) {
        this.skillsFallbackReason = skillsFallbackReason;
    }

    public String getSkillsModelName() {
        return skillsModelName;
    }

    public void setSkillsModelName(String skillsModelName) {
        this.skillsModelName = skillsModelName;
    }

    public String getOptimizedProjects() {
        return optimizedProjects;
    }

    public void setOptimizedProjects(String optimizedProjects) {
        this.optimizedProjects = optimizedProjects;
    }

    public String getProjectsGenerationSource() {
        return projectsGenerationSource;
    }

    public void setProjectsGenerationSource(String projectsGenerationSource) {
        this.projectsGenerationSource = projectsGenerationSource;
    }

    public String getProjectsFallbackReason() {
        return projectsFallbackReason;
    }

    public void setProjectsFallbackReason(String projectsFallbackReason) {
        this.projectsFallbackReason = projectsFallbackReason;
    }

    public String getProjectsModelName() {
        return projectsModelName;
    }

    public void setProjectsModelName(String projectsModelName) {
        this.projectsModelName = projectsModelName;
    }

    public String getOptimizedExperience() {
        return optimizedExperience;
    }

    public void setOptimizedExperience(String optimizedExperience) {
        this.optimizedExperience = optimizedExperience;
    }

    public String getExperienceGenerationSource() {
        return experienceGenerationSource;
    }

    public void setExperienceGenerationSource(String experienceGenerationSource) {
        this.experienceGenerationSource = experienceGenerationSource;
    }

    public String getExperienceFallbackReason() {
        return experienceFallbackReason;
    }

    public void setExperienceFallbackReason(String experienceFallbackReason) {
        this.experienceFallbackReason = experienceFallbackReason;
    }

    public String getExperienceModelName() {
        return experienceModelName;
    }

    public void setExperienceModelName(String experienceModelName) {
        this.experienceModelName = experienceModelName;
    }

    public String getOptimizedOthers() {
        return optimizedOthers;
    }

    public void setOptimizedOthers(String optimizedOthers) {
        this.optimizedOthers = optimizedOthers;
    }

    public String getOthersGenerationSource() {
        return othersGenerationSource;
    }

    public void setOthersGenerationSource(String othersGenerationSource) {
        this.othersGenerationSource = othersGenerationSource;
    }

    public String getOthersFallbackReason() {
        return othersFallbackReason;
    }

    public void setOthersFallbackReason(String othersFallbackReason) {
        this.othersFallbackReason = othersFallbackReason;
    }

    public String getOthersModelName() {
        return othersModelName;
    }

    public void setOthersModelName(String othersModelName) {
        this.othersModelName = othersModelName;
    }

    public String getFullOptimizedResume() {
        return fullOptimizedResume;
    }

    public void setFullOptimizedResume(String fullOptimizedResume) {
        this.fullOptimizedResume = fullOptimizedResume;
    }

    public String getFullResumeGenerationSource() {
        return fullResumeGenerationSource;
    }

    public void setFullResumeGenerationSource(String fullResumeGenerationSource) {
        this.fullResumeGenerationSource = fullResumeGenerationSource;
    }

    public String getFullResumeFallbackReason() {
        return fullResumeFallbackReason;
    }

    public void setFullResumeFallbackReason(String fullResumeFallbackReason) {
        this.fullResumeFallbackReason = fullResumeFallbackReason;
    }

    public String getFullResumeModelName() {
        return fullResumeModelName;
    }

    public void setFullResumeModelName(String fullResumeModelName) {
        this.fullResumeModelName = fullResumeModelName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
