package com.zhihang.jobagent.dto;

import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.UserProfile;

import java.util.List;

public class CareerProfileCenterView {

    private final UserProfile profile;
    private final JobPost selectedJob;
    private final ProfileOverview overview;
    private final ProfileCompleteness completeness;
    private final ResumeBackflow resumeBackflow;
    private final NextStepRecommendation nextStepRecommendation;
    private final List<ActionLink> secondaryActions;
    private final List<RecentSummaryItem> recentSummaryItems;
    private final String syncNotice;

    public CareerProfileCenterView(UserProfile profile,
                                   JobPost selectedJob,
                                   ProfileOverview overview,
                                   ProfileCompleteness completeness,
                                   ResumeBackflow resumeBackflow,
                                   NextStepRecommendation nextStepRecommendation,
                                   List<ActionLink> secondaryActions,
                                   List<RecentSummaryItem> recentSummaryItems,
                                   String syncNotice) {
        this.profile = profile;
        this.selectedJob = selectedJob;
        this.overview = overview;
        this.completeness = completeness;
        this.resumeBackflow = resumeBackflow;
        this.nextStepRecommendation = nextStepRecommendation;
        this.secondaryActions = List.copyOf(secondaryActions);
        this.recentSummaryItems = List.copyOf(recentSummaryItems);
        this.syncNotice = syncNotice;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public JobPost getSelectedJob() {
        return selectedJob;
    }

    public ProfileOverview getOverview() {
        return overview;
    }

    public ProfileCompleteness getCompleteness() {
        return completeness;
    }

    public ResumeBackflow getResumeBackflow() {
        return resumeBackflow;
    }

    public NextStepRecommendation getNextStepRecommendation() {
        return nextStepRecommendation;
    }

    public List<ActionLink> getSecondaryActions() {
        return secondaryActions;
    }

    public List<RecentSummaryItem> getRecentSummaryItems() {
        return recentSummaryItems;
    }

    public String getSyncNotice() {
        return syncNotice;
    }

    public boolean isHasProfile() {
        return profile != null;
    }

    public static class ProfileOverview {

        private final String displayName;
        private final String targetRole;
        private final String targetDirection;
        private final String targetCity;
        private final String expectedSalary;
        private final String jobNature;
        private final String updatedAtLabel;
        private final String targetSnapshot;

        public ProfileOverview(String displayName,
                               String targetRole,
                               String targetDirection,
                               String targetCity,
                               String expectedSalary,
                               String jobNature,
                               String updatedAtLabel,
                               String targetSnapshot) {
            this.displayName = displayName;
            this.targetRole = targetRole;
            this.targetDirection = targetDirection;
            this.targetCity = targetCity;
            this.expectedSalary = expectedSalary;
            this.jobNature = jobNature;
            this.updatedAtLabel = updatedAtLabel;
            this.targetSnapshot = targetSnapshot;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTargetRole() {
            return targetRole;
        }

        public String getTargetDirection() {
            return targetDirection;
        }

        public String getTargetCity() {
            return targetCity;
        }

        public String getExpectedSalary() {
            return expectedSalary;
        }

        public String getJobNature() {
            return jobNature;
        }

        public String getUpdatedAtLabel() {
            return updatedAtLabel;
        }

        public String getTargetSnapshot() {
            return targetSnapshot;
        }
    }

    public static class ProfileCompleteness {

        private final int completionPercent;
        private final String completionLabel;
        private final String prioritySuggestion;
        private final List<String> missingItems;
        private final List<DimensionStatus> dimensionStatuses;

        public ProfileCompleteness(int completionPercent,
                                   String completionLabel,
                                   String prioritySuggestion,
                                   List<String> missingItems,
                                   List<DimensionStatus> dimensionStatuses) {
            this.completionPercent = completionPercent;
            this.completionLabel = completionLabel;
            this.prioritySuggestion = prioritySuggestion;
            this.missingItems = List.copyOf(missingItems);
            this.dimensionStatuses = List.copyOf(dimensionStatuses);
        }

        public int getCompletionPercent() {
            return completionPercent;
        }

        public String getCompletionLabel() {
            return completionLabel;
        }

        public String getPrioritySuggestion() {
            return prioritySuggestion;
        }

        public List<String> getMissingItems() {
            return missingItems;
        }

        public List<DimensionStatus> getDimensionStatuses() {
            return dimensionStatuses;
        }

        public boolean isReadyForMatching() {
            return completionPercent >= 70 && missingItems.stream().noneMatch(item ->
                    item.contains("目标岗位") || item.contains("技能") || item.contains("项目"));
        }
    }

    public static class DimensionStatus {

        private final String label;
        private final String description;
        private final boolean complete;

        public DimensionStatus(String label, String description, boolean complete) {
            this.label = label;
            this.description = description;
            this.complete = complete;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }

        public boolean isComplete() {
            return complete;
        }
    }

    public static class ResumeBackflow {

        private final boolean available;
        private final boolean draftCreation;
        private final boolean canApply;
        private final Long resumeRecordId;
        private final String uploadedAtLabel;
        private final String inputTypeLabel;
        private final String skillSummary;
        private final String projectSummary;
        private final String targetRoleCandidate;
        private final String targetCityCandidate;
        private final String careerGoalCandidate;
        private final String acceptableJobNatureCandidate;
        private final String targetIndustryCandidate;
        private final String syncStatusText;
        private final String applyButtonLabel;
        private final List<ProfileSyncFieldSuggestion> fieldSuggestions;
        private final int recommendedCount;

        public ResumeBackflow(boolean available,
                              boolean draftCreation,
                              boolean canApply,
                              Long resumeRecordId,
                              String uploadedAtLabel,
                              String inputTypeLabel,
                              String skillSummary,
                              String projectSummary,
                              String targetRoleCandidate,
                              String targetCityCandidate,
                              String careerGoalCandidate,
                              String acceptableJobNatureCandidate,
                              String targetIndustryCandidate,
                              String syncStatusText,
                              String applyButtonLabel,
                              List<ProfileSyncFieldSuggestion> fieldSuggestions,
                              int recommendedCount) {
            this.available = available;
            this.draftCreation = draftCreation;
            this.canApply = canApply;
            this.resumeRecordId = resumeRecordId;
            this.uploadedAtLabel = uploadedAtLabel;
            this.inputTypeLabel = inputTypeLabel;
            this.skillSummary = skillSummary;
            this.projectSummary = projectSummary;
            this.targetRoleCandidate = targetRoleCandidate;
            this.targetCityCandidate = targetCityCandidate;
            this.careerGoalCandidate = careerGoalCandidate;
            this.acceptableJobNatureCandidate = acceptableJobNatureCandidate;
            this.targetIndustryCandidate = targetIndustryCandidate;
            this.syncStatusText = syncStatusText;
            this.applyButtonLabel = applyButtonLabel;
            this.fieldSuggestions = List.copyOf(fieldSuggestions);
            this.recommendedCount = recommendedCount;
        }

        public boolean isAvailable() {
            return available;
        }

        public boolean isDraftCreation() {
            return draftCreation;
        }

        public boolean isCanApply() {
            return canApply;
        }

        public Long getResumeRecordId() {
            return resumeRecordId;
        }

        public String getUploadedAtLabel() {
            return uploadedAtLabel;
        }

        public String getInputTypeLabel() {
            return inputTypeLabel;
        }

        public String getSkillSummary() {
            return skillSummary;
        }

        public String getProjectSummary() {
            return projectSummary;
        }

        public String getTargetRoleCandidate() {
            return targetRoleCandidate;
        }

        public String getTargetCityCandidate() {
            return targetCityCandidate;
        }

        public String getCareerGoalCandidate() {
            return careerGoalCandidate;
        }

        public String getAcceptableJobNatureCandidate() {
            return acceptableJobNatureCandidate;
        }

        public String getTargetIndustryCandidate() {
            return targetIndustryCandidate;
        }

        public String getSyncStatusText() {
            return syncStatusText;
        }

        public String getApplyButtonLabel() {
            return applyButtonLabel;
        }

        public List<ProfileSyncFieldSuggestion> getFieldSuggestions() {
            return fieldSuggestions;
        }

        public int getRecommendedCount() {
            return recommendedCount;
        }
    }

    public static class ActionLink {

        private final String label;
        private final String description;
        private final String href;

        public ActionLink(String label, String description, String href) {
            this.label = label;
            this.description = description;
            this.href = href;
        }

        public String getLabel() {
            return label;
        }

        public String getDescription() {
            return description;
        }

        public String getHref() {
            return href;
        }
    }

    public static class RecentSummaryItem {

        private final String title;
        private final boolean available;
        private final String timeLabel;
        private final String primaryText;
        private final String secondaryText;
        private final String tertiaryText;

        public RecentSummaryItem(String title,
                                 boolean available,
                                 String timeLabel,
                                 String primaryText,
                                 String secondaryText,
                                 String tertiaryText) {
            this.title = title;
            this.available = available;
            this.timeLabel = timeLabel;
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
            this.tertiaryText = tertiaryText;
        }

        public String getTitle() {
            return title;
        }

        public boolean isAvailable() {
            return available;
        }

        public String getTimeLabel() {
            return timeLabel;
        }

        public String getPrimaryText() {
            return primaryText;
        }

        public String getSecondaryText() {
            return secondaryText;
        }

        public String getTertiaryText() {
            return tertiaryText;
        }
    }
}
