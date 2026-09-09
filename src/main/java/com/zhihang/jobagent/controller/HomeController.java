package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.dto.InterviewSessionSummary;
import com.zhihang.jobagent.dto.ProfileJourneySnapshot;
import com.zhihang.jobagent.entity.InterviewRecord;
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
import com.zhihang.jobagent.repository.UpdateLogRepository;
import com.zhihang.jobagent.repository.ResumeReviewRepository;
import com.zhihang.jobagent.repository.UserProfileRepository;
import com.zhihang.jobagent.service.CurrentUserService;
import com.zhihang.jobagent.service.InterviewService;
import com.zhihang.jobagent.service.JourneyAdvisorService;
import com.zhihang.jobagent.service.UserAccessService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class HomeController {

    private final JobPostRepository jobPostRepository;
    private final UserProfileRepository userProfileRepository;
    private final JobMatchRecordRepository jobMatchRecordRepository;
    private final ResumeReviewRepository resumeReviewRepository;
    private final ResumeEnhanceRecordRepository resumeEnhanceRecordRepository;
    private final LearningPathAdviceRepository learningPathAdviceRepository;
    private final InterviewRecordRepository interviewRecordRepository;
    private final InterviewService interviewService;
    private final JourneyAdvisorService journeyAdvisorService;
    private final UpdateLogRepository updateLogRepository;
    private final CurrentUserService currentUserService;
    private final UserAccessService userAccessService;

    public HomeController(JobPostRepository jobPostRepository,
                          UserProfileRepository userProfileRepository,
                          JobMatchRecordRepository jobMatchRecordRepository,
                          ResumeReviewRepository resumeReviewRepository,
                          ResumeEnhanceRecordRepository resumeEnhanceRecordRepository,
                          LearningPathAdviceRepository learningPathAdviceRepository,
                          InterviewRecordRepository interviewRecordRepository,
                          InterviewService interviewService,
                          JourneyAdvisorService journeyAdvisorService,
                          UpdateLogRepository updateLogRepository,
                          CurrentUserService currentUserService,
                          UserAccessService userAccessService) {
        this.jobPostRepository = jobPostRepository;
        this.userProfileRepository = userProfileRepository;
        this.jobMatchRecordRepository = jobMatchRecordRepository;
        this.resumeReviewRepository = resumeReviewRepository;
        this.resumeEnhanceRecordRepository = resumeEnhanceRecordRepository;
        this.learningPathAdviceRepository = learningPathAdviceRepository;
        this.interviewRecordRepository = interviewRecordRepository;
        this.interviewService = interviewService;
        this.journeyAdvisorService = journeyAdvisorService;
        this.updateLogRepository = updateLogRepository;
        this.currentUserService = currentUserService;
        this.userAccessService = userAccessService;
    }

    @GetMapping("/")
    public String showDashboard(Model model) {
        fillStats(model);
        if (currentUserService.isAuthenticated() && !currentUserService.isAdmin()) {
            fillJourneySnapshot(model);
        }
        return "dashboard";
    }

    @GetMapping("/history")
    public String redirectHistoryToHistoryCenter() {
        return "redirect:/history-center";
    }

    @GetMapping("/my-records")
    public String redirectMyRecordsToHistoryCenter() {
        return "redirect:/history-center";
    }

    @GetMapping("/records")
    public String redirectRecordsToHistoryCenter() {
        return "redirect:/history-center";
    }

    @GetMapping("/about")
    public String showAboutPage(Model model) {
        fillStats(model);
        return "about";
    }

    @GetMapping("/admin")
    public String showAdminHub(Model model) {
        fillStats(model);
        return "admin/index";
    }

    @GetMapping("/question-bank")
    public String redirectQuestionBank() {
        return "redirect:/admin/questions";
    }

    @GetMapping("/forbidden")
    public String showForbiddenPage() {
        return "forbidden";
    }

    private void fillStats(Model model) {
        model.addAttribute("jobCount", jobPostRepository.count());
        model.addAttribute("profileCount", userProfileRepository.count());
        model.addAttribute("resumeReviewCount", resumeReviewRepository.count());
        model.addAttribute("interviewSessionCount", calculateInterviewSessionCount());
        model.addAttribute("lastJobUpdateLog", updateLogRepository.findTopByUpdateTypeOrderByCreatedAtDescIdDesc("JOB").orElse(null));
        model.addAttribute("lastQuestionUpdateLog", updateLogRepository.findTopByUpdateTypeOrderByCreatedAtDescIdDesc("QUESTION").orElse(null));
        model.addAttribute("lastResourceUpdateLog", updateLogRepository.findTopByUpdateTypeOrderByCreatedAtDescIdDesc("RESOURCE").orElse(null));
        model.addAttribute("latestUpdateLogs", updateLogRepository.findTop20ByOrderByCreatedAtDescIdDesc().stream().limit(5).toList());
    }

    private void fillJourneySnapshot(Model model) {
        List<UserProfile> profiles = userAccessService.findAccessibleProfiles();
        if (profiles.isEmpty()) {
            model.addAttribute("homeJourneySnapshot", journeyAdvisorService.buildSnapshot(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
            return;
        }

        UserProfile focusProfile = profiles.get(0);
        Long profileId = focusProfile.getId();
        JobMatchRecord latestMatch = firstOrNull(jobMatchRecordRepository.findTop10ByUserProfileIdOrderByCreatedAtDescIdDesc(profileId));
        ResumeReview latestReview = firstOrNull(resumeReviewRepository.findTop10ByUserProfileIdOrderByIdDesc(profileId));
        ResumeEnhanceRecord latestEnhance = firstOrNull(resumeEnhanceRecordRepository.findTop10ByUserProfileIdOrderByCreatedAtDescIdDesc(profileId));
        LearningPathAdvice latestStrengthPlan = firstOrNull(learningPathAdviceRepository.findTop10ByUserProfileIdOrderByCreatedAtDescIdDesc(profileId));
        InterviewSessionSummary latestInterviewSession = resolveLatestInterviewSession(profileId);
        Long currentJobId = resolveSelectedJobId(latestMatch, latestStrengthPlan, latestEnhance, latestReview, latestInterviewSession);
        JobPost currentJob = currentJobId == null ? null : jobPostRepository.findById(currentJobId).orElse(null);

        ProfileJourneySnapshot snapshot = journeyAdvisorService.buildSnapshot(
                focusProfile,
                currentJob,
                latestMatch,
                latestReview,
                latestEnhance,
                latestStrengthPlan,
                latestInterviewSession,
                null
        );
        model.addAttribute("homeJourneySnapshot", snapshot);
    }

    private long calculateInterviewSessionCount() {
        return interviewRecordRepository.findAll().stream()
                .map(InterviewRecord::getSessionId)
                .filter(StringUtils::hasText)
                .distinct()
                .count();
    }

    private Long resolveSelectedJobId(JobMatchRecord latestMatch,
                                      LearningPathAdvice latestStrengthPlan,
                                      ResumeEnhanceRecord latestEnhance,
                                      ResumeReview latestReview,
                                      InterviewSessionSummary latestInterviewSession) {
        if (latestMatch != null) {
            return latestMatch.getJobPostId();
        }
        if (latestStrengthPlan != null) {
            return latestStrengthPlan.getTargetJobId();
        }
        if (latestEnhance != null) {
            return latestEnhance.getJobPostId();
        }
        if (latestReview != null) {
            return latestReview.getJobPostId();
        }
        if (latestInterviewSession != null) {
            return latestInterviewSession.getJobPostId();
        }
        return null;
    }

    private InterviewSessionSummary resolveLatestInterviewSession(Long userProfileId) {
        List<InterviewSessionSummary> summaries = interviewService.buildSessionSummaries(
                interviewRecordRepository.findTop30ByUserProfileIdOrderByCreatedAtDescIdDesc(userProfileId)
        );
        return firstOrNull(summaries);
    }

    private <T> T firstOrNull(List<T> values) {
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
