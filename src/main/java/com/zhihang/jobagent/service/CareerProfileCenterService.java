package com.zhihang.jobagent.service;

import com.zhihang.jobagent.dto.CareerProfileCenterView;
import com.zhihang.jobagent.dto.InterviewSessionSummary;
import com.zhihang.jobagent.dto.NextStepRecommendation;
import com.zhihang.jobagent.dto.ProfileSyncFieldSuggestion;
import com.zhihang.jobagent.dto.ResumeParsedData;
import com.zhihang.jobagent.dto.ResumeProfileSyncSuggestion;
import com.zhihang.jobagent.entity.JobMatchRecord;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.LearningPathAdvice;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.InterviewRecordRepository;
import com.zhihang.jobagent.repository.JobMatchRecordRepository;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.LearningPathAdviceRepository;
import com.zhihang.jobagent.repository.ResumeEnhanceRecordRepository;
import com.zhihang.jobagent.repository.ResumeReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CareerProfileCenterService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final JobPostRepository jobPostRepository;
    private final JobMatchRecordRepository jobMatchRecordRepository;
    private final ResumeReviewRepository resumeReviewRepository;
    private final ResumeEnhanceRecordRepository resumeEnhanceRecordRepository;
    private final LearningPathAdviceRepository learningPathAdviceRepository;
    private final InterviewRecordRepository interviewRecordRepository;
    private final InterviewService interviewService;
    private final ResumeProfileSyncService resumeProfileSyncService;
    private final CurrentUserService currentUserService;

    public CareerProfileCenterService(JobPostRepository jobPostRepository,
                                      JobMatchRecordRepository jobMatchRecordRepository,
                                      ResumeReviewRepository resumeReviewRepository,
                                      ResumeEnhanceRecordRepository resumeEnhanceRecordRepository,
                                      LearningPathAdviceRepository learningPathAdviceRepository,
                                      InterviewRecordRepository interviewRecordRepository,
                                      InterviewService interviewService,
                                      ResumeProfileSyncService resumeProfileSyncService,
                                      CurrentUserService currentUserService) {
        this.jobPostRepository = jobPostRepository;
        this.jobMatchRecordRepository = jobMatchRecordRepository;
        this.resumeReviewRepository = resumeReviewRepository;
        this.resumeEnhanceRecordRepository = resumeEnhanceRecordRepository;
        this.learningPathAdviceRepository = learningPathAdviceRepository;
        this.interviewRecordRepository = interviewRecordRepository;
        this.interviewService = interviewService;
        this.resumeProfileSyncService = resumeProfileSyncService;
        this.currentUserService = currentUserService;
    }

    public CareerProfileCenterView build(UserProfile selectedProfile, Long requestedJobId, String syncNotice) {
        JobMatchRecord latestMatch = selectedProfile == null
                ? firstOrNull(loadUserMatchRecords())
                : firstOrNull(jobMatchRecordRepository.findTop10ByUserProfileIdOrderByCreatedAtDescIdDesc(selectedProfile.getId()));
        ResumeReview latestReview = selectedProfile == null
                ? firstOrNull(loadUserResumeReviews())
                : firstOrNull(resumeReviewRepository.findTop10ByUserProfileIdOrderByIdDesc(selectedProfile.getId()));
        ResumeEnhanceRecord latestEnhance = selectedProfile == null
                ? firstOrNull(loadUserResumeEnhanceRecords())
                : firstOrNull(resumeEnhanceRecordRepository.findTop10ByUserProfileIdOrderByCreatedAtDescIdDesc(selectedProfile.getId()));
        LearningPathAdvice latestLearningPath = selectedProfile == null
                ? firstOrNull(loadUserLearningPaths())
                : firstOrNull(learningPathAdviceRepository.findTop10ByUserProfileIdOrderByCreatedAtDescIdDesc(selectedProfile.getId()));
        InterviewSessionSummary latestInterview = selectedProfile == null
                ? resolveLatestInterviewSessionByUser()
                : resolveLatestInterviewSessionByProfile(selectedProfile.getId());

        Long selectedJobId = resolveSelectedJobId(requestedJobId, latestMatch, latestLearningPath, latestEnhance, latestReview, latestInterview);
        JobPost selectedJob = selectedJobId == null ? null : jobPostRepository.findById(selectedJobId).orElse(null);
        ResumeProfileSyncSuggestion syncSuggestion = buildSyncSuggestion(selectedProfile, latestEnhance, selectedJob);

        return new CareerProfileCenterView(
                selectedProfile,
                selectedJob,
                buildOverview(selectedProfile, syncSuggestion),
                buildCompleteness(selectedProfile, latestEnhance),
                buildResumeBackflow(selectedProfile, latestEnhance, syncSuggestion),
                buildNextStep(selectedProfile, selectedJob, latestMatch, latestEnhance, latestLearningPath, latestInterview, syncSuggestion),
                buildSecondaryActions(selectedProfile, selectedJobId),
                buildRecentSummaryItems(selectedProfile, latestMatch, latestEnhance, latestLearningPath, latestInterview, syncSuggestion),
                syncNotice
        );
    }

    private CareerProfileCenterView.ProfileOverview buildOverview(UserProfile profile, ResumeProfileSyncSuggestion syncSuggestion) {
        String displayName = profile == null ? "未建立当前档案" : display(profile.getStudentName(), "当前档案");
        String targetRole = profile == null
                ? candidate(syncSuggestion == null ? null : syncSuggestion.getRecognizedTargetRole())
                : display(profile.getTargetRole(), candidate(syncSuggestion == null ? null : syncSuggestion.getRecognizedTargetRole()));
        String targetDirection = profile == null
                ? candidate(syncSuggestion == null ? null : syncSuggestion.getRecognizedDirection())
                : display(profile.getTargetDirection(), candidate(syncSuggestion == null ? null : syncSuggestion.getRecognizedDirection()));
        String targetCity = profile == null
                ? candidate(syncSuggestion == null ? null : syncSuggestion.getRecognizedTargetCity())
                : display(profile.getTargetCity(), candidate(syncSuggestion == null ? null : syncSuggestion.getRecognizedTargetCity()));
        String expectedSalary = profile == null
                ? candidate(syncSuggestion == null ? null : syncSuggestion.getRecognizedExpectedSalary())
                : display(profile.getExpectedSalary(), candidate(syncSuggestion == null ? null : syncSuggestion.getRecognizedExpectedSalary()));
        String jobNature = profile == null
                ? candidate(syncSuggestion == null ? null : syncSuggestion.getRecognizedJobNature())
                : display(profile.getAcceptableJobNature(), candidate(syncSuggestion == null ? null : syncSuggestion.getRecognizedJobNature()));
        String updatedAtLabel = profile == null || profile.getUpdatedAt() == null
                ? "待创建"
                : DATE_TIME_FORMATTER.format(profile.getUpdatedAt());
        String targetSnapshot = buildTargetSnapshot(targetRole, targetDirection, targetCity, expectedSalary);
        return new CareerProfileCenterView.ProfileOverview(
                displayName,
                targetRole,
                targetDirection,
                targetCity,
                expectedSalary,
                jobNature,
                updatedAtLabel,
                targetSnapshot
        );
    }

    private CareerProfileCenterView.ProfileCompleteness buildCompleteness(UserProfile profile, ResumeEnhanceRecord latestEnhance) {
        List<CareerProfileCenterView.DimensionStatus> dimensions = new ArrayList<>();
        List<String> missingItems = new ArrayList<>();
        int completionPercent = 0;

        boolean basicComplete = profile != null
                && hasText(profile.getStudentName())
                && hasText(profile.getGrade())
                && hasText(profile.getMajor());
        dimensions.add(new CareerProfileCenterView.DimensionStatus(
                "基础信息",
                basicComplete ? "姓名、年级、专业已具备" : "姓名、年级或专业仍待补全",
                basicComplete
        ));
        if (basicComplete) {
            completionPercent += 18;
        } else {
            missingItems.add("基础信息待补全");
        }

        boolean skillsComplete = profile != null && hasMeaningfulText(profile.getSkills(), 4);
        dimensions.add(new CareerProfileCenterView.DimensionStatus(
                "技能信息",
                skillsComplete ? "已有技能关键词，可支持岗位匹配" : "技能关键词还不够完整",
                skillsComplete
        ));
        if (skillsComplete) {
            completionPercent += 15;
        } else {
            missingItems.add("技能信息待补全");
        }

        boolean projectComplete = profile != null && hasMeaningfulText(profile.getProjectExperience(), 12);
        dimensions.add(new CareerProfileCenterView.DimensionStatus(
                "项目经历",
                projectComplete ? "已有项目或实习摘要" : "缺少项目经历支撑",
                projectComplete
        ));
        if (projectComplete) {
            completionPercent += 15;
        } else {
            missingItems.add("项目经历待补全");
        }

        boolean targetRoleComplete = profile != null && (hasText(profile.getTargetRole()) || hasText(profile.getTargetDirection()));
        dimensions.add(new CareerProfileCenterView.DimensionStatus(
                "目标岗位",
                targetRoleComplete ? "目标岗位方向已经明确" : "目标岗位仍不够明确",
                targetRoleComplete
        ));
        if (targetRoleComplete) {
            completionPercent += 15;
        } else {
            missingItems.add("目标岗位待明确");
        }

        boolean cityComplete = profile != null && hasText(profile.getTargetCity());
        dimensions.add(new CareerProfileCenterView.DimensionStatus(
                "城市偏好",
                cityComplete ? "目标城市已明确" : "目标城市尚未填写",
                cityComplete
        ));
        if (cityComplete) {
            completionPercent += 10;
        } else {
            missingItems.add("城市偏好待明确");
        }

        boolean salaryComplete = profile != null && hasText(profile.getExpectedSalary());
        dimensions.add(new CareerProfileCenterView.DimensionStatus(
                "薪资期望",
                salaryComplete ? "薪资期望已沉淀到档案" : "薪资期望尚未填写",
                salaryComplete
        ));
        if (salaryComplete) {
            completionPercent += 10;
        } else {
            missingItems.add("薪资期望待明确");
        }

        boolean resumeRecognized = latestEnhance != null && (
                hasText(latestEnhance.getParsedBasicInfo())
                        || hasText(latestEnhance.getParsedSkills())
                        || hasText(latestEnhance.getParsedProjects())
                        || hasText(latestEnhance.getOriginalResumeText())
        );
        dimensions.add(new CareerProfileCenterView.DimensionStatus(
                "简历识别",
                resumeRecognized ? "最近一次简历识别已可回流档案" : "还没有最近简历识别结果",
                resumeRecognized
        ));
        if (resumeRecognized) {
            completionPercent += 17;
        } else {
            missingItems.add("简历待上传或识别");
        }

        return new CareerProfileCenterView.ProfileCompleteness(
                completionPercent,
                completenessLabel(completionPercent),
                buildPrioritySuggestion(missingItems),
                missingItems,
                dimensions
        );
    }

    private CareerProfileCenterView.ResumeBackflow buildResumeBackflow(UserProfile profile,
                                                                       ResumeEnhanceRecord latestEnhance,
                                                                       ResumeProfileSyncSuggestion syncSuggestion) {
        if (latestEnhance == null) {
            return new CareerProfileCenterView.ResumeBackflow(
                    false,
                    profile == null,
                    false,
                    null,
                    "暂无记录",
                    "待上传",
                    "暂无技能识别结果",
                    "暂无项目识别结果",
                    "—",
                    "—",
                    "—",
                    "—",
                    "—",
                    "先上传简历，识别结果会优先回流到这里",
                    "去上传简历",
                    List.of(),
                    0
            );
        }

        List<ProfileSyncFieldSuggestion> visibleSuggestions = syncSuggestion == null
                ? List.of()
                : syncSuggestion.getFieldSuggestions().stream()
                .filter(item -> item.isRecommended() || !"—".equals(item.getRecognizedValue()))
                .toList();
        boolean canApply = profile == null || (syncSuggestion != null && syncSuggestion.hasRecommendedUpdates());
        String syncStatusText;
        if (profile == null) {
            syncStatusText = "最近一次识别结果可直接创建草稿档案";
        } else if (syncSuggestion != null && syncSuggestion.hasRecommendedUpdates()) {
            syncStatusText = "检测到 " + syncSuggestion.getRecommendedCount() + " 项可补全信息，只会补空白字段";
        } else {
            syncStatusText = "当前档案已覆盖最近一次识别结果";
        }

        return new CareerProfileCenterView.ResumeBackflow(
                true,
                profile == null,
                canApply,
                latestEnhance.getId(),
                formatDateTime(latestEnhance.getCreatedAt(), "刚刚完成"),
                readableInputType(latestEnhance.getInputType()),
                syncSuggestion == null ? "暂无技能识别结果" : display(syncSuggestion.getSkillSummary(), "暂无技能识别结果"),
                syncSuggestion == null ? "暂无项目识别结果" : display(syncSuggestion.getProjectSummary(), "暂无项目识别结果"),
                syncSuggestion == null ? "—" : candidate(syncSuggestion.getRecognizedTargetRole()),
                syncSuggestion == null ? "—" : candidate(syncSuggestion.getRecognizedTargetCity()),
                syncSuggestion == null ? "—" : candidate(syncSuggestion.getRecognizedCareerGoal()),
                syncSuggestion == null ? "—" : candidate(syncSuggestion.getRecognizedJobNature()),
                syncSuggestion == null ? "—" : candidate(syncSuggestion.getRecognizedIndustry()),
                syncStatusText,
                profile == null ? "从简历创建草稿档案" : "一键补全当前档案",
                visibleSuggestions,
                syncSuggestion == null ? 0 : syncSuggestion.getRecommendedCount()
        );
    }

    private NextStepRecommendation buildNextStep(UserProfile profile,
                                                 JobPost selectedJob,
                                                 JobMatchRecord latestMatch,
                                                 ResumeEnhanceRecord latestEnhance,
                                                 LearningPathAdvice latestLearningPath,
                                                 InterviewSessionSummary latestInterview,
                                                 ResumeProfileSyncSuggestion syncSuggestion) {
        if (profile == null) {
            if (latestEnhance != null) {
                return new NextStepRecommendation(
                        "create-profile",
                        "创建当前档案",
                        "最近一次简历识别已经完成，先把候选信息沉淀成当前档案，再继续后面的匹配和补强。",
                        "去创建当前档案",
                        "#resume-backflow"
                );
            }
            return new NextStepRecommendation(
                    "create-profile",
                    "补全求职档案",
                    "当前还没有可用档案，先建立你的求职目标和基础信息，后续功能都会从这里读取。",
                    "新建求职档案",
                    "/profiles/new"
            );
        }

        CareerProfileCenterView.ProfileCompleteness completeness = buildCompleteness(profile, latestEnhance);
        boolean profileIncomplete = !completeness.isReadyForMatching();
        if (profileIncomplete) {
            return new NextStepRecommendation(
                    "complete-profile",
                    "补全求职档案",
                    "当前档案仍缺少关键输入，建议先补全目标岗位、技能或项目经历，再进入岗位匹配。",
                    "去补全档案",
                    "/profiles/edit/" + profile.getId()
            );
        }
        if (latestMatch == null) {
            return new NextStepRecommendation(
                    "job-match",
                    "开始岗位匹配",
                    "当前档案已经具备基本输入，下一步先验证哪些岗位最值得优先投递。",
                    "开始岗位匹配",
                    "/match/" + profile.getId()
            );
        }
        if (latestEnhance == null) {
            return new NextStepRecommendation(
                    "resume-optimize",
                    "去做简历优化",
                    "已有岗位匹配结果，接下来先把简历版本和岗位方向对齐，再继续推进补强。",
                    "去做简历优化",
                    buildResumeOptimizeHref(profile.getId(), selectedJob == null ? null : selectedJob.getId())
            );
        }
        if (latestLearningPath == null) {
            return new NextStepRecommendation(
                    "strength-plan",
                    "生成补强方案",
                    "简历优化结果已经形成，接下来把短板整理成明确的补强计划。",
                    "生成补强方案",
                    buildStrengthPlanHref(profile.getId(), selectedJob == null ? null : selectedJob.getId())
            );
        }
        if (latestInterview == null) {
            return new NextStepRecommendation(
                    "interview",
                    "开始模拟面试",
                    "补强方向已经明确，下一步用模拟面试验证当前准备程度。",
                    "开始模拟面试",
                    buildInterviewHref(profile.getId(), selectedJob == null ? null : selectedJob.getId())
            );
        }
        return new NextStepRecommendation(
                "history",
                "查看我的记录",
                "当前主线已经形成闭环，接下来从记录里回看哪些动作最有效，再继续迭代。",
                "查看我的记录",
                "/history-center"
        );
    }

    private List<CareerProfileCenterView.ActionLink> buildSecondaryActions(UserProfile profile, Long selectedJobId) {
        if (profile == null) {
            return List.of(
                    new CareerProfileCenterView.ActionLink("简历优化", "先上传或识别简历", "/resume-optimize"),
                    new CareerProfileCenterView.ActionLink("我的记录", "回看已完成动作", "/history-center")
            );
        }
        return List.of(
                new CareerProfileCenterView.ActionLink("岗位匹配", "读取当前档案做匹配", "/match/" + profile.getId()),
                new CareerProfileCenterView.ActionLink("简历优化", "围绕当前岗位方向优化简历", buildResumeOptimizeHref(profile.getId(), selectedJobId)),
                new CareerProfileCenterView.ActionLink("补强方案", "针对短板生成计划", buildStrengthPlanHref(profile.getId(), selectedJobId)),
                new CareerProfileCenterView.ActionLink("模拟面试", "按目标岗位做训练", buildInterviewHref(profile.getId(), selectedJobId)),
                new CareerProfileCenterView.ActionLink("我的记录", "回看最近推进主线", "/history-center")
        );
    }

    private List<CareerProfileCenterView.RecentSummaryItem> buildRecentSummaryItems(UserProfile profile,
                                                                                    JobMatchRecord latestMatch,
                                                                                    ResumeEnhanceRecord latestEnhance,
                                                                                    LearningPathAdvice latestLearningPath,
                                                                                    InterviewSessionSummary latestInterview,
                                                                                    ResumeProfileSyncSuggestion syncSuggestion) {
        List<CareerProfileCenterView.RecentSummaryItem> items = new ArrayList<>();
        items.add(buildMatchSummary(profile, latestMatch));
        items.add(buildResumeSummary(latestEnhance, syncSuggestion));
        items.add(buildLearningSummary(latestLearningPath));
        items.add(buildInterviewSummary(latestInterview));
        return items;
    }

    private CareerProfileCenterView.RecentSummaryItem buildMatchSummary(UserProfile profile, JobMatchRecord latestMatch) {
        if (latestMatch == null) {
            return new CareerProfileCenterView.RecentSummaryItem("最近一次岗位匹配", false, "暂无记录", "还没有匹配结果", "建议先完成岗位匹配", "—");
        }
        List<JobMatchRecord> sameRun = profile == null || latestMatch.getCreatedAt() == null
                ? List.of(latestMatch)
                : jobMatchRecordRepository.findByUserProfileIdAndCreatedAtOrderByIdAsc(profile.getId(), latestMatch.getCreatedAt());
        int recommendedJobs = sameRun.isEmpty() ? 1 : sameRun.size();
        int highestScore = sameRun.stream()
                .map(JobMatchRecord::getMatchScore)
                .filter(score -> score != null)
                .max(Comparator.naturalOrder())
                .orElse(latestMatch.getMatchScore() == null ? 0 : latestMatch.getMatchScore());
        return new CareerProfileCenterView.RecentSummaryItem(
                "最近一次岗位匹配",
                true,
                formatDateTime(latestMatch.getCreatedAt(), "时间未知"),
                "推荐岗位数 " + recommendedJobs,
                "最高匹配分 " + highestScore,
                display(latestMatch.getCompatibilityLabel(), "已生成匹配结果")
        );
    }

    private CareerProfileCenterView.RecentSummaryItem buildResumeSummary(ResumeEnhanceRecord latestEnhance,
                                                                         ResumeProfileSyncSuggestion syncSuggestion) {
        if (latestEnhance == null) {
            return new CareerProfileCenterView.RecentSummaryItem("最近一次简历优化", false, "暂无记录", "还没有简历优化结果", "建议先做简历优化", "—");
        }
        String syncStatus = syncSuggestion == null || !syncSuggestion.hasRecommendedUpdates() ? "已同步档案" : "待确认同步";
        String scoreText = latestEnhance.getReviewScore() == null ? "评分待生成" : "总体评分 " + latestEnhance.getReviewScore();
        return new CareerProfileCenterView.RecentSummaryItem(
                "最近一次简历优化",
                true,
                formatDateTime(latestEnhance.getCreatedAt(), "时间未知"),
                scoreText,
                syncStatus,
                readableInputType(latestEnhance.getInputType())
        );
    }

    private CareerProfileCenterView.RecentSummaryItem buildLearningSummary(LearningPathAdvice latestLearningPath) {
        if (latestLearningPath == null) {
            return new CareerProfileCenterView.RecentSummaryItem("最近一次补强方案", false, "暂无记录", "还没有补强方案", "建议先生成补强方案", "—");
        }
        return new CareerProfileCenterView.RecentSummaryItem(
                "最近一次补强方案",
                true,
                formatDateTime(latestLearningPath.getCreatedAt(), "时间未知"),
                display(latestLearningPath.getTargetDirection(), "目标岗位待确认"),
                display(latestLearningPath.getEstimatedDuration(), "学习时长待生成"),
                display(latestLearningPath.getSuggestedLevel(), "已生成阶段建议")
        );
    }

    private CareerProfileCenterView.RecentSummaryItem buildInterviewSummary(InterviewSessionSummary latestInterview) {
        if (latestInterview == null) {
            return new CareerProfileCenterView.RecentSummaryItem("最近一次模拟面试", false, "暂无记录", "还没有模拟面试记录", "建议先开始模拟面试", "—");
        }
        return new CareerProfileCenterView.RecentSummaryItem(
                "最近一次模拟面试",
                true,
                formatDateTime(latestInterview.getCreatedAt(), "时间未知"),
                "总分 " + defaultNumber(latestInterview.getTotalScore()),
                "题目数 " + defaultNumber(latestInterview.getQuestionCount()),
                "可继续查看答题记录"
        );
    }

    private ResumeProfileSyncSuggestion buildSyncSuggestion(UserProfile selectedProfile,
                                                            ResumeEnhanceRecord latestEnhance,
                                                            JobPost selectedJob) {
        if (latestEnhance == null) {
            return null;
        }
        ResumeParsedData parsedData = resumeProfileSyncService.toParsedData(latestEnhance);
        return parsedData == null ? null : resumeProfileSyncService.buildSuggestion(selectedProfile, parsedData, selectedJob);
    }

    private Long resolveSelectedJobId(Long requestedJobId,
                                      JobMatchRecord latestMatch,
                                      LearningPathAdvice latestLearningPath,
                                      ResumeEnhanceRecord latestEnhance,
                                      ResumeReview latestReview,
                                      InterviewSessionSummary latestInterview) {
        if (requestedJobId != null) {
            return requestedJobId;
        }
        if (latestMatch != null) {
            return latestMatch.getJobPostId();
        }
        if (latestLearningPath != null) {
            return latestLearningPath.getTargetJobId();
        }
        if (latestEnhance != null) {
            return latestEnhance.getJobPostId();
        }
        if (latestReview != null) {
            return latestReview.getJobPostId();
        }
        return latestInterview == null ? null : latestInterview.getJobPostId();
    }

    private InterviewSessionSummary resolveLatestInterviewSessionByProfile(Long profileId) {
        return firstOrNull(interviewService.buildSessionSummaries(
                interviewRecordRepository.findTop30ByUserProfileIdOrderByCreatedAtDescIdDesc(profileId)
        ));
    }

    private InterviewSessionSummary resolveLatestInterviewSessionByUser() {
        if (!currentUserService.isAuthenticated()) {
            return null;
        }
        return firstOrNull(interviewService.buildSessionSummaries(
                interviewRecordRepository.findTop50ByUserAccountIdOrderByCreatedAtDescIdDesc(currentUserService.requireCurrentUserId())
        ));
    }

    private List<JobMatchRecord> loadUserMatchRecords() {
        if (!currentUserService.isAuthenticated()) {
            return List.of();
        }
        return jobMatchRecordRepository.findTop20ByUserAccountIdOrderByCreatedAtDescIdDesc(currentUserService.requireCurrentUserId());
    }

    private List<ResumeReview> loadUserResumeReviews() {
        if (!currentUserService.isAuthenticated()) {
            return List.of();
        }
        return resumeReviewRepository.findTop20ByUserAccountIdOrderByIdDesc(currentUserService.requireCurrentUserId());
    }

    private List<ResumeEnhanceRecord> loadUserResumeEnhanceRecords() {
        if (!currentUserService.isAuthenticated()) {
            return List.of();
        }
        return resumeEnhanceRecordRepository.findTop20ByUserAccountIdOrderByCreatedAtDescIdDesc(currentUserService.requireCurrentUserId());
    }

    private List<LearningPathAdvice> loadUserLearningPaths() {
        if (!currentUserService.isAuthenticated()) {
            return List.of();
        }
        return learningPathAdviceRepository.findTop20ByUserAccountIdOrderByCreatedAtDescIdDesc(currentUserService.requireCurrentUserId());
    }

    private String buildTargetSnapshot(String targetRole, String direction, String city, String salary) {
        List<String> parts = new ArrayList<>();
        if (isConcreteValue(targetRole)) {
            parts.add(targetRole);
        }
        if (isConcreteValue(direction) && !direction.equals(targetRole)) {
            parts.add(direction);
        }
        if (isConcreteValue(city)) {
            parts.add(city);
        }
        if (isConcreteValue(salary)) {
            parts.add(salary);
        }
        return parts.isEmpty() ? "当前目标仍待明确" : String.join(" / ", parts);
    }

    private String buildPrioritySuggestion(List<String> missingItems) {
        if (missingItems.stream().anyMatch(item -> item.contains("目标岗位"))) {
            return "先明确目标岗位和方向，再继续后面的匹配与补强。";
        }
        if (missingItems.stream().anyMatch(item -> item.contains("技能")) || missingItems.stream().anyMatch(item -> item.contains("项目"))) {
            return "先补齐技能和项目经历，让岗位匹配和补强输入更稳定。";
        }
        if (missingItems.stream().anyMatch(item -> item.contains("简历"))) {
            return "先上传简历，利用识别结果回流补齐当前档案。";
        }
        if (missingItems.stream().anyMatch(item -> item.contains("城市")) || missingItems.stream().anyMatch(item -> item.contains("薪资"))) {
            return "补齐城市与薪资偏好，方便系统缩小匹配范围。";
        }
        return "当前档案已经可以继续推进，建议直接进入下一步。";
    }

    private String completenessLabel(int completionPercent) {
        if (completionPercent >= 85) {
            return "已具备投递基础";
        }
        if (completionPercent >= 70) {
            return "主线输入基本齐备";
        }
        if (completionPercent >= 45) {
            return "还需要补全关键项";
        }
        return "仍处于起步阶段";
    }

    private String readableInputType(String inputType) {
        String normalized = defaultText(inputType).toLowerCase(Locale.ROOT);
        if (normalized.contains("pdf")) {
            return "PDF 简历";
        }
        if (normalized.contains("text")) {
            return "文本导入";
        }
        return hasText(inputType) ? inputType.trim() : "未知来源";
    }

    private String buildResumeOptimizeHref(Long profileId, Long jobId) {
        StringBuilder builder = new StringBuilder("/resume-optimize?profileId=").append(profileId);
        if (jobId != null) {
            builder.append("&jobId=").append(jobId);
        }
        builder.append("&source=career-profile");
        return builder.toString();
    }

    private String buildStrengthPlanHref(Long profileId, Long jobId) {
        StringBuilder builder = new StringBuilder("/strength-plan?userProfileId=").append(profileId);
        if (jobId != null) {
            builder.append("&targetJobId=").append(jobId);
        }
        builder.append("&source=career-profile");
        return builder.toString();
    }

    private String buildInterviewHref(Long profileId, Long jobId) {
        StringBuilder builder = new StringBuilder("/interview?profileId=").append(profileId);
        if (jobId != null) {
            builder.append("&jobId=").append(jobId);
        }
        builder.append("&source=career-profile");
        return builder.toString();
    }

    private String formatDateTime(LocalDateTime dateTime, String fallback) {
        return dateTime == null ? fallback : DATE_TIME_FORMATTER.format(dateTime);
    }

    private String defaultNumber(Integer value) {
        return value == null ? "0" : String.valueOf(value);
    }

    private String candidate(String value) {
        return display(value, "待识别");
    }

    private String display(String value, String fallback) {
        return hasText(value) ? value.trim() : fallback;
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private boolean hasMeaningfulText(String value, int minLength) {
        return hasText(value) && value.trim().length() >= minLength;
    }

    private boolean isConcreteValue(String value) {
        return hasText(value) && !"—".equals(value) && !value.startsWith("待");
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private <T> T firstOrNull(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
