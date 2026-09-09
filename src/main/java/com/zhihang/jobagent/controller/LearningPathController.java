package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.dto.LearningPathResultView;
import com.zhihang.jobagent.entity.JobMatchRecord;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.LearningPathAdvice;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.JobMatchRecordRepository;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.LearningPathAdviceRepository;
import com.zhihang.jobagent.repository.ResumeEnhanceRecordRepository;
import com.zhihang.jobagent.repository.ResumeReviewRepository;
import com.zhihang.jobagent.repository.UserProfileRepository;
import com.zhihang.jobagent.service.CurrentUserService;
import com.zhihang.jobagent.service.LearningPathService;
import com.zhihang.jobagent.service.UserAccessService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Controller
public class LearningPathController {

    private final UserProfileRepository userProfileRepository;
    private final JobPostRepository jobPostRepository;
    private final ResumeReviewRepository resumeReviewRepository;
    private final ResumeEnhanceRecordRepository resumeEnhanceRecordRepository;
    private final JobMatchRecordRepository jobMatchRecordRepository;
    private final LearningPathAdviceRepository learningPathAdviceRepository;
    private final LearningPathService learningPathService;
    private final UserAccessService userAccessService;
    private final CurrentUserService currentUserService;

    public LearningPathController(UserProfileRepository userProfileRepository,
                                  JobPostRepository jobPostRepository,
                                  ResumeReviewRepository resumeReviewRepository,
                                  ResumeEnhanceRecordRepository resumeEnhanceRecordRepository,
                                  JobMatchRecordRepository jobMatchRecordRepository,
                                  LearningPathAdviceRepository learningPathAdviceRepository,
                                  LearningPathService learningPathService,
                                  UserAccessService userAccessService,
                                  CurrentUserService currentUserService) {
        this.userProfileRepository = userProfileRepository;
        this.jobPostRepository = jobPostRepository;
        this.resumeReviewRepository = resumeReviewRepository;
        this.resumeEnhanceRecordRepository = resumeEnhanceRecordRepository;
        this.jobMatchRecordRepository = jobMatchRecordRepository;
        this.learningPathAdviceRepository = learningPathAdviceRepository;
        this.learningPathService = learningPathService;
        this.userAccessService = userAccessService;
        this.currentUserService = currentUserService;
    }

    @GetMapping({"/learning-path", "/strength-plan"})
    public String showForm(@RequestParam(required = false) Long profileId,
                           @RequestParam(required = false) Long userProfileId,
                           @RequestParam(required = false) Long jobId,
                           @RequestParam(required = false) Long targetJobId,
                           @RequestParam(required = false) String source,
                           Model model) {
        Long selectedProfileId = userProfileId != null ? userProfileId : profileId;
        Long selectedJobId = targetJobId != null ? targetJobId : jobId;
        model.addAttribute("profiles", userAccessService.findAccessibleProfiles());
        model.addAttribute("jobs", jobPostRepository.findAll());
        model.addAttribute("selectedProfileId", selectedProfileId);
        model.addAttribute("selectedJobId", selectedJobId);
        model.addAttribute("source", source);
        return "learning-path/form";
    }

    @PostMapping({"/learning-path/generate", "/strength-plan/generate"})
    public String generate(@RequestParam Long userProfileId,
                           @RequestParam Long targetJobId,
                           @RequestParam(required = false) String source,
                           Model model) {
        ResultBundle resultBundle = generateAdvice(userProfileId, targetJobId);
        fillResultModel(model, resultBundle, source);
        return "learning-path/result";
    }

    @PostMapping({"/learning-path/regenerate-json", "/strength-plan/regenerate-json"})
    @ResponseBody
    public Map<String, Object> regenerateJson(@RequestParam Long userProfileId,
                                              @RequestParam Long targetJobId,
                                              @RequestParam(required = false) String source) {
        ResultBundle resultBundle = generateAdvice(userProfileId, targetJobId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("source", source);
        payload.put("adviceId", resultBundle.advice().getId());
        payload.put("jobName", resultBundle.jobPost().getJobName());
        payload.put("jobDirection", resultBundle.jobPost().getJobDirection());
        payload.put("profileName", resultBundle.userProfile().getStudentName());
        payload.put("profileDirection", resultBundle.userProfile().getTargetDirection());
        payload.put("weaknessSourceSummary", resultBundle.resultView().getWeaknessSourceSummary());
        payload.put("estimatedDuration", resultBundle.advice().getEstimatedDuration());
        payload.put("deliveryReadiness", resultBundle.advice().getDeliveryReadiness());
        payload.put("createdAt", resultBundle.advice().getCreatedAt() == null ? "" : resultBundle.advice().getCreatedAt().toString());
        payload.put("learningStepsGenerationSource", resultBundle.advice().getLearningStepsGenerationSource());
        payload.put("learningStepsSourceLabel", sourceLabel(resultBundle.advice().getLearningStepsGenerationSource()));
        payload.put("learningStepsModelName", resultBundle.advice().getLearningStepsModelName());
        payload.put("learningStepsFallbackReason", resultBundle.advice().getLearningStepsFallbackReason());
        payload.put("coachGenerationSource", resultBundle.advice().getCoachGenerationSource());
        payload.put("coachSourceLabel", sourceLabel(resultBundle.advice().getCoachGenerationSource()));
        payload.put("coachModelName", resultBundle.advice().getCoachModelName());
        payload.put("coachFallbackReason", resultBundle.advice().getCoachFallbackReason());
        payload.put("suggestedLevel", resultBundle.advice().getSuggestedLevel());
        payload.put("profileWeaknesses", resultBundle.resultView().getProfileWeaknesses());
        payload.put("matchWeaknesses", resultBundle.resultView().getMatchWeaknesses());
        payload.put("resumeWeaknesses", resultBundle.resultView().getResumeWeaknesses());
        payload.put("preferenceSummary", resultBundle.resultView().getPreferenceSummary());
        payload.put("coachSummary", resultBundle.resultView().getCoachSummary());
        payload.put("impactExplanation", resultBundle.resultView().getImpactExplanation());
        payload.put("priorityFocus", resultBundle.resultView().getPriorityFocus());
        payload.put("skillOrProjectFirst", resultBundle.resultView().getSkillOrProjectFirst());
        payload.put("deliveryTimingAdvice", resultBundle.resultView().getDeliveryTimingAdvice());
        payload.put("nextActionSuggestion", resultBundle.resultView().getNextActionSuggestion());
        payload.put("learningSteps", resultBundle.resultView().getLearningSteps());
        payload.put("recommendedProjects", resultBundle.resultView().getRecommendedProjects());
        payload.put("suggestedOrder", resultBundle.resultView().getSuggestedOrder());
        payload.put("resourceTypeSuggestions", resultBundle.resultView().getResourceTypeSuggestions());
        payload.put("recommendedResources", resultBundle.resultView().getRecommendedResources());
        payload.put("deliveryChecklist", resultBundle.resultView().getDeliveryChecklist());
        payload.put("resumeSignalsMissing", resultBundle.resultView().isResumeSignalsMissing());
        return payload;
    }

    @GetMapping({"/learning-path/history", "/strength-plan/history"})
    public String showHistory(Model model) {
        List<LearningPathAdvice> records = loadCurrentUserRecords();
        model.addAttribute("records", records);
        model.addAttribute("userNameMap", buildUserNameMap());
        model.addAttribute("jobNameMap", buildJobNameMap());
        model.addAttribute("relatedMatchSummaryMap", buildRelatedMatchSummaryMap(records));
        model.addAttribute("relatedMatchIdMap", buildRelatedMatchIdMap(records));
        model.addAttribute("relatedResumeEnhanceIdMap", buildRelatedResumeEnhanceIdMap(records));
        return "learning-path/history";
    }

    @GetMapping({"/learning-path/{id}", "/strength-plan/{id}"})
    public String showDetail(@PathVariable Long id, Model model) {
        LearningPathAdvice advice = learningPathAdviceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid strength plan id: " + id));
        userAccessService.assertCanAccessRecord(advice.getUserAccountId(), advice.getUserProfileId());

        UserProfile userProfile = userProfileRepository.findById(advice.getUserProfileId()).orElse(null);
        JobPost jobPost = jobPostRepository.findById(advice.getTargetJobId()).orElse(null);

        JobMatchRecord latestMatch = null;
        ResumeReview latestReview = null;
        ResumeEnhanceRecord latestEnhanceRecord = null;
        if (advice.getUserProfileId() != null && advice.getTargetJobId() != null) {
            latestMatch = findLatestMatch(advice.getUserProfileId(), advice.getTargetJobId());
            latestReview = findLatestReview(advice.getUserProfileId(), advice.getTargetJobId());
            latestEnhanceRecord = findLatestEnhance(advice.getUserProfileId(), advice.getTargetJobId());
        }

        LearningPathResultView resultView = buildResultView(userProfile, jobPost, advice, latestMatch, latestReview, latestEnhanceRecord);
        fillResultModel(model, new ResultBundle(advice, userProfile, jobPost, latestMatch, latestReview, latestEnhanceRecord, resultView), null);
        return "learning-path/result";
    }

    private ResultBundle generateAdvice(Long userProfileId, Long targetJobId) {
        UserProfile userProfile = userAccessService.requireAccessibleProfile(userProfileId);
        JobPost jobPost = jobPostRepository.findById(targetJobId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid job id: " + targetJobId));

        JobMatchRecord latestMatch = findLatestMatch(userProfileId, targetJobId);
        ResumeReview latestReview = findLatestReview(userProfileId, targetJobId);
        ResumeEnhanceRecord latestEnhanceRecord = findLatestEnhance(userProfileId, targetJobId);

        LearningPathAdvice advice = learningPathService.generate(
                userProfile, jobPost, latestMatch, latestReview, latestEnhanceRecord
        );
        LearningPathAdvice savedAdvice = learningPathAdviceRepository.save(advice);
        LearningPathResultView resultView = buildResultView(
                userProfile,
                jobPost,
                savedAdvice,
                latestMatch,
                latestReview,
                latestEnhanceRecord
        );
        return new ResultBundle(savedAdvice, userProfile, jobPost, latestMatch, latestReview, latestEnhanceRecord, resultView);
    }

    private void fillResultModel(Model model, ResultBundle resultBundle, String source) {
        model.addAttribute("advice", resultBundle.advice());
        model.addAttribute("userProfile", resultBundle.userProfile());
        model.addAttribute("jobPost", resultBundle.jobPost());
        model.addAttribute("source", source);
        model.addAttribute("resultView", resultBundle.resultView());
        model.addAttribute("coachSourceLabel", sourceLabel(resultBundle.advice().getCoachGenerationSource()));
    }

    private LearningPathResultView buildResultView(UserProfile userProfile, JobPost jobPost, LearningPathAdvice advice,
                                                   JobMatchRecord latestMatch, ResumeReview latestReview,
                                                   ResumeEnhanceRecord latestEnhanceRecord) {
        if (userProfile == null || jobPost == null) {
            return new LearningPathResultView(
                    List.of("当前档案信息还不完整。"),
                    List.of(),
                    List.of(),
                    List.of("先补齐求职档案和目标岗位，再生成补强方案会更准确。"),
                    List.of("先准备一个能证明岗位能力的项目作品。"),
                    List.of("先补档案", "再补简历", "最后重新匹配"),
                    List.of("官方文档", "项目实战", "模拟面试"),
                    List.of(),
                    List.of("建议先完成档案补全、简历优化和一次面试练习，再集中投递。"),
                    "岗位 0 项 / 档案 1 项 / 简历 0 项",
                    "暂未读取到有效偏好",
                    "先补齐档案和目标岗位，再生成定制化补强方案。",
                    "当前缺少完整的岗位、档案或简历上下文，AI 教练建议会不够准确。",
                    "先补档案与目标岗位",
                    "BALANCED",
                    "补齐档案后再生成补强方案。",
                    "先去完善求职档案，再回到补强方案。",
                    true
            );
        }
        return learningPathService.buildResultView(userProfile, jobPost, advice, latestMatch, latestReview, latestEnhanceRecord);
    }

    private String sourceLabel(String source) {
        if ("GEMINI".equalsIgnoreCase(source)) {
            return "Gemini";
        }
        if ("QWEN".equalsIgnoreCase(source)) {
            return "Qwen";
        }
        if ("FALLBACK".equalsIgnoreCase(source)) {
            return "Fallback";
        }
        return "Pending";
    }

    private JobMatchRecord findLatestMatch(Long userProfileId, Long targetJobId) {
        return jobMatchRecordRepository
                .findTopByUserProfileIdAndJobPostIdOrderByCreatedAtDescIdDesc(userProfileId, targetJobId)
                .orElse(null);
    }

    private ResumeReview findLatestReview(Long userProfileId, Long targetJobId) {
        return resumeReviewRepository
                .findTopByUserProfileIdAndJobPostIdOrderByIdDesc(userProfileId, targetJobId)
                .orElse(null);
    }

    private ResumeEnhanceRecord findLatestEnhance(Long userProfileId, Long targetJobId) {
        return resumeEnhanceRecordRepository
                .findTopByUserProfileIdAndJobPostIdOrderByCreatedAtDescIdDesc(userProfileId, targetJobId)
                .orElse(null);
    }

    private Map<Long, String> buildUserNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (UserProfile profile : userAccessService.findAccessibleProfiles()) {
            map.put(profile.getId(), profile.getStudentName());
        }
        return map;
    }

    private Map<Long, String> buildJobNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (JobPost jobPost : jobPostRepository.findAll()) {
            map.put(jobPost.getId(), jobPost.getJobName());
        }
        return map;
    }

    private Map<Long, String> buildRelatedMatchSummaryMap(List<LearningPathAdvice> records) {
        Map<Long, String> map = new HashMap<>();
        for (LearningPathAdvice record : records) {
            if (record.getUserProfileId() == null || record.getTargetJobId() == null) {
                map.put(record.getId(), "暂无关联岗位匹配");
                continue;
            }
            JobMatchRecord matchRecord = findLatestMatch(record.getUserProfileId(), record.getTargetJobId());
            if (matchRecord == null) {
                map.put(record.getId(), "暂无关联岗位匹配");
            } else {
                map.put(record.getId(), "匹配记录 #" + matchRecord.getId() + " / 分数 "
                        + (matchRecord.getMatchScore() == null ? "-" : matchRecord.getMatchScore()));
            }
        }
        return map;
    }

    private Map<Long, Long> buildRelatedMatchIdMap(List<LearningPathAdvice> records) {
        Map<Long, Long> map = new HashMap<>();
        for (LearningPathAdvice record : records) {
            if (record.getUserProfileId() == null || record.getTargetJobId() == null) {
                continue;
            }
            JobMatchRecord matchRecord = findLatestMatch(record.getUserProfileId(), record.getTargetJobId());
            if (matchRecord != null) {
                map.put(record.getId(), matchRecord.getId());
            }
        }
        return map;
    }

    private Map<Long, Long> buildRelatedResumeEnhanceIdMap(List<LearningPathAdvice> records) {
        Map<Long, Long> map = new HashMap<>();
        for (LearningPathAdvice record : records) {
            if (record.getUserProfileId() == null || record.getTargetJobId() == null) {
                continue;
            }
            ResumeEnhanceRecord resumeEnhanceRecord = findLatestEnhance(record.getUserProfileId(), record.getTargetJobId());
            if (resumeEnhanceRecord != null) {
                map.put(record.getId(), resumeEnhanceRecord.getId());
            }
        }
        return map;
    }

    private List<LearningPathAdvice> loadCurrentUserRecords() {
        Long userId = currentUserService.requireCurrentUserId();
        List<Long> profileIds = userAccessService.findAccessibleProfileIds();
        return Stream.concat(
                        learningPathAdviceRepository.findTop20ByUserAccountIdOrderByCreatedAtDescIdDesc(userId).stream(),
                        profileIds.isEmpty()
                                ? Stream.empty()
                                : learningPathAdviceRepository.findTop20ByUserProfileIdInAndUserAccountIdIsNullOrderByCreatedAtDescIdDesc(profileIds).stream()
                )
                .distinct()
                .limit(20)
                .toList();
    }

    private record ResultBundle(LearningPathAdvice advice,
                                UserProfile userProfile,
                                JobPost jobPost,
                                JobMatchRecord latestMatch,
                                ResumeReview latestReview,
                                ResumeEnhanceRecord latestEnhanceRecord,
                                LearningPathResultView resultView) {
    }
}
