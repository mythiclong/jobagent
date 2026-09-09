package com.zhihang.jobagent.service;

import com.zhihang.jobagent.dto.InterviewSessionSummary;
import com.zhihang.jobagent.dto.NextStepRecommendation;
import com.zhihang.jobagent.dto.ProfileJourneySnapshot;
import com.zhihang.jobagent.dto.ProfileStatusSummary;
import com.zhihang.jobagent.entity.JobMatchRecord;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.LearningPathAdvice;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.entity.UserProfile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JourneyAdvisorService {

    public ProfileJourneySnapshot buildSnapshot(UserProfile profile,
                                                JobPost currentJob,
                                                JobMatchRecord latestMatch,
                                                ResumeReview latestReview,
                                                ResumeEnhanceRecord latestEnhance,
                                                LearningPathAdvice latestStrengthPlan,
                                                InterviewSessionSummary latestInterviewSession,
                                                String syncNotice) {
        ProfileStatusSummary statusSummary = buildStatusSummary(profile, currentJob, latestEnhance, syncNotice);
        NextStepRecommendation nextStepRecommendation = buildNextStepRecommendation(
                profile,
                currentJob,
                latestMatch,
                latestEnhance,
                latestStrengthPlan,
                latestInterviewSession
        );
        return new ProfileJourneySnapshot(
                profile,
                currentJob,
                statusSummary,
                nextStepRecommendation,
                latestMatch,
                latestReview,
                latestEnhance,
                latestStrengthPlan,
                latestInterviewSession
        );
    }

    private ProfileStatusSummary buildStatusSummary(UserProfile profile,
                                                    JobPost currentJob,
                                                    ResumeEnhanceRecord latestEnhance,
                                                    String syncNotice) {
        if (profile == null) {
            return new ProfileStatusSummary(
                    0,
                    "尚未建立档案",
                    "尚未形成当前目标岗位",
                    "基础信息待补全",
                    "技能信息待补全",
                    "项目经历待补充",
                    "暂无简历识别记录",
                    StringUtils.hasText(syncNotice) ? syncNotice : "暂无档案同步动作"
            );
        }

        boolean basicInfoComplete = hasText(profile.getStudentName())
                && hasText(profile.getGrade())
                && hasText(profile.getMajor())
                && hasText(profile.getTargetDirection());
        boolean skillsComplete = hasText(profile.getSkills());
        boolean projectReady = hasText(profile.getProjectExperience());
        boolean hasTargetJob = currentJob != null;
        boolean hasResumeRecognition = latestEnhance != null && (
                hasText(latestEnhance.getParsedBasicInfo())
                        || hasText(latestEnhance.getParsedSkills())
                        || hasText(latestEnhance.getParsedProjects())
        );

        int completionPercent = 0;
        if (basicInfoComplete) {
            completionPercent += 30;
        }
        if (skillsComplete) {
            completionPercent += 20;
        }
        if (projectReady) {
            completionPercent += 20;
        }
        if (hasTargetJob) {
            completionPercent += 10;
        }
        if (hasResumeRecognition) {
            completionPercent += 20;
        }

        return new ProfileStatusSummary(
                completionPercent,
                completionLabel(completionPercent),
                hasTargetJob ? "已带入当前目标岗位" : "尚未形成当前目标岗位",
                basicInfoComplete ? "基础信息已完整" : "基础信息仍待补全",
                skillsComplete ? "技能信息已具备" : "技能信息仍待补全",
                projectReady ? "项目经历可直接参与匹配" : "项目经历仍然偏少",
                hasResumeRecognition ? "最近一次简历识别已完成" : "暂无简历识别记录",
                hasText(syncNotice)
                        ? syncNotice
                        : (hasResumeRecognition ? "最近一次识别结果可同步到求职档案" : "暂无档案同步动作")
        );
    }

    private NextStepRecommendation buildNextStepRecommendation(UserProfile profile,
                                                               JobPost currentJob,
                                                               JobMatchRecord latestMatch,
                                                               ResumeEnhanceRecord latestEnhance,
                                                               LearningPathAdvice latestStrengthPlan,
                                                               InterviewSessionSummary latestInterviewSession) {
        if (profile == null) {
            return new NextStepRecommendation(
                    "create-profile",
                    "先建立求职档案",
                    "当前还没有可用档案，建议先补齐基础信息、技能和目标方向，再进入后续求职准备。",
                    "新建求职档案",
                    "/profiles/new"
            );
        }

        if (latestMatch == null) {
            return new NextStepRecommendation(
                    "job-match",
                    "开始岗位匹配",
                    "当前档案和目标岗位信息已具备基础条件，建议先生成岗位匹配结果，确认最值得优先准备的岗位。",
                    "开始岗位匹配",
                    "/match/" + profile.getId()
            );
        }

        if (latestEnhance == null) {
            return new NextStepRecommendation(
                    "resume-optimize",
                    "去做简历优化",
                    "系统已识别目标岗位和当前短板，建议先优化简历，再生成更准确的补强方案。",
                    "去简历优化",
                    buildResumeOptimizeHref(profile.getId(), currentJob == null ? null : currentJob.getId(), "career-profile")
            );
        }

        if (latestStrengthPlan == null) {
            return new NextStepRecommendation(
                    "strength-plan",
                    "生成补强方案",
                    "当前岗位方向和简历问题已明确，建议生成补强方案，确定接下来优先补哪些能力和项目。",
                    "生成补强方案",
                    buildStrengthPlanHref(profile.getId(), currentJob == null ? null : currentJob.getId(), "career-profile")
            );
        }

        if (latestInterviewSession == null) {
            return new NextStepRecommendation(
                    "interview",
                    "开始模拟面试",
                    "当前已形成岗位方向、简历版本和补强建议，建议通过模拟面试验证准备效果。",
                    "开始模拟面试",
                    buildInterviewHref(profile.getId(), currentJob == null ? null : currentJob.getId(), "career-profile")
            );
        }

        return new NextStepRecommendation(
                "history",
                "查看我的记录",
                "你已经形成了较完整的求职准备链路，可以回顾匹配、补强、简历优化和模拟面试记录，继续迭代。",
                "查看我的记录",
                "/history-center"
        );
    }

    private String completionLabel(int completionPercent) {
        if (completionPercent >= 80) {
            return "准备较完整";
        }
        if (completionPercent >= 60) {
            return "基础条件已具备";
        }
        if (completionPercent >= 40) {
            return "仍需补全";
        }
        return "起步阶段";
    }

    private String buildResumeOptimizeHref(Long profileId, Long jobId, String source) {
        StringBuilder builder = new StringBuilder("/resume-optimize");
        boolean hasQuery = false;
        if (profileId != null) {
            builder.append("?profileId=").append(profileId);
            hasQuery = true;
        }
        if (jobId != null) {
            builder.append(hasQuery ? "&" : "?").append("jobId=").append(jobId);
            hasQuery = true;
        }
        if (StringUtils.hasText(source)) {
            builder.append(hasQuery ? "&" : "?").append("source=").append(source);
        }
        return builder.toString();
    }

    private String buildStrengthPlanHref(Long profileId, Long jobId, String source) {
        StringBuilder builder = new StringBuilder("/strength-plan");
        boolean hasQuery = false;
        if (profileId != null) {
            builder.append("?userProfileId=").append(profileId);
            hasQuery = true;
        }
        if (jobId != null) {
            builder.append(hasQuery ? "&" : "?").append("targetJobId=").append(jobId);
            hasQuery = true;
        }
        if (StringUtils.hasText(source)) {
            builder.append(hasQuery ? "&" : "?").append("source=").append(source);
        }
        return builder.toString();
    }

    private String buildInterviewHref(Long profileId, Long jobId, String source) {
        StringBuilder builder = new StringBuilder("/interview");
        boolean hasQuery = false;
        if (profileId != null) {
            builder.append("?profileId=").append(profileId);
            hasQuery = true;
        }
        if (jobId != null) {
            builder.append(hasQuery ? "&" : "?").append("jobId=").append(jobId);
            hasQuery = true;
        }
        if (StringUtils.hasText(source)) {
            builder.append(hasQuery ? "&" : "?").append("source=").append(source);
        }
        return builder.toString();
    }

    private boolean hasText(String text) {
        return StringUtils.hasText(text);
    }
}
