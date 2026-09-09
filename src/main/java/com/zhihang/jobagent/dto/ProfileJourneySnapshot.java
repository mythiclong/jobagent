package com.zhihang.jobagent.dto;

import com.zhihang.jobagent.entity.JobMatchRecord;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.LearningPathAdvice;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.entity.UserProfile;

public class ProfileJourneySnapshot {

    private final UserProfile profile;
    private final JobPost currentJob;
    private final ProfileStatusSummary statusSummary;
    private final NextStepRecommendation nextStepRecommendation;
    private final JobMatchRecord latestMatch;
    private final ResumeReview latestReview;
    private final ResumeEnhanceRecord latestEnhance;
    private final LearningPathAdvice latestStrengthPlan;
    private final InterviewSessionSummary latestInterviewSession;

    public ProfileJourneySnapshot(UserProfile profile,
                                  JobPost currentJob,
                                  ProfileStatusSummary statusSummary,
                                  NextStepRecommendation nextStepRecommendation,
                                  JobMatchRecord latestMatch,
                                  ResumeReview latestReview,
                                  ResumeEnhanceRecord latestEnhance,
                                  LearningPathAdvice latestStrengthPlan,
                                  InterviewSessionSummary latestInterviewSession) {
        this.profile = profile;
        this.currentJob = currentJob;
        this.statusSummary = statusSummary;
        this.nextStepRecommendation = nextStepRecommendation;
        this.latestMatch = latestMatch;
        this.latestReview = latestReview;
        this.latestEnhance = latestEnhance;
        this.latestStrengthPlan = latestStrengthPlan;
        this.latestInterviewSession = latestInterviewSession;
    }

    public UserProfile getProfile() {
        return profile;
    }

    public JobPost getCurrentJob() {
        return currentJob;
    }

    public ProfileStatusSummary getStatusSummary() {
        return statusSummary;
    }

    public NextStepRecommendation getNextStepRecommendation() {
        return nextStepRecommendation;
    }

    public JobMatchRecord getLatestMatch() {
        return latestMatch;
    }

    public ResumeReview getLatestReview() {
        return latestReview;
    }

    public ResumeEnhanceRecord getLatestEnhance() {
        return latestEnhance;
    }

    public LearningPathAdvice getLatestStrengthPlan() {
        return latestStrengthPlan;
    }

    public InterviewSessionSummary getLatestInterviewSession() {
        return latestInterviewSession;
    }
}
