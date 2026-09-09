package com.zhihang.jobagent.dto;

public class ProfileStatusSummary {

    private final int completionPercent;
    private final String completionLabel;
    private final String targetJobStatus;
    private final String basicInfoStatus;
    private final String skillsStatus;
    private final String projectStatus;
    private final String latestResumeStatus;
    private final String latestSyncStatus;

    public ProfileStatusSummary(int completionPercent,
                                String completionLabel,
                                String targetJobStatus,
                                String basicInfoStatus,
                                String skillsStatus,
                                String projectStatus,
                                String latestResumeStatus,
                                String latestSyncStatus) {
        this.completionPercent = completionPercent;
        this.completionLabel = completionLabel;
        this.targetJobStatus = targetJobStatus;
        this.basicInfoStatus = basicInfoStatus;
        this.skillsStatus = skillsStatus;
        this.projectStatus = projectStatus;
        this.latestResumeStatus = latestResumeStatus;
        this.latestSyncStatus = latestSyncStatus;
    }

    public int getCompletionPercent() {
        return completionPercent;
    }

    public String getCompletionLabel() {
        return completionLabel;
    }

    public String getTargetJobStatus() {
        return targetJobStatus;
    }

    public String getBasicInfoStatus() {
        return basicInfoStatus;
    }

    public String getSkillsStatus() {
        return skillsStatus;
    }

    public String getProjectStatus() {
        return projectStatus;
    }

    public String getLatestResumeStatus() {
        return latestResumeStatus;
    }

    public String getLatestSyncStatus() {
        return latestSyncStatus;
    }
}
