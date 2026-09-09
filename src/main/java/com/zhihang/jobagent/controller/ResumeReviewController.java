package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.ResumeReviewRepository;
import com.zhihang.jobagent.repository.UserProfileRepository;
import com.zhihang.jobagent.service.CurrentUserService;
import com.zhihang.jobagent.service.ResumeReviewService;
import com.zhihang.jobagent.service.UserAccessService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Controller
public class ResumeReviewController {

    private final UserProfileRepository userProfileRepository;
    private final JobPostRepository jobPostRepository;
    private final ResumeReviewRepository resumeReviewRepository;
    private final ResumeReviewService resumeReviewService;
    private final UserAccessService userAccessService;
    private final CurrentUserService currentUserService;

    public ResumeReviewController(UserProfileRepository userProfileRepository,
                                  JobPostRepository jobPostRepository,
                                  ResumeReviewRepository resumeReviewRepository,
                                  ResumeReviewService resumeReviewService,
                                  UserAccessService userAccessService,
                                  CurrentUserService currentUserService) {
        this.userProfileRepository = userProfileRepository;
        this.jobPostRepository = jobPostRepository;
        this.resumeReviewRepository = resumeReviewRepository;
        this.resumeReviewService = resumeReviewService;
        this.userAccessService = userAccessService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/resume-review")
    public String showForm(@RequestParam(required = false) Long profileId,
                           @RequestParam(required = false) Long jobId,
                           @RequestParam(required = false) String source,
                           Model model) {
        model.addAttribute("profiles", userAccessService.findAccessibleProfiles());
        model.addAttribute("jobs", jobPostRepository.findAll());
        model.addAttribute("selectedProfileId", profileId);
        model.addAttribute("selectedJobId", jobId);
        model.addAttribute("source", source);
        return "resume/form";
    }

    @PostMapping("/resume-review/analyze")
    public String analyzeResume(@RequestParam Long userProfileId,
                                @RequestParam Long jobPostId,
                                @RequestParam String resumeText,
                                Model model) {
        UserProfile userProfile = userAccessService.requireAccessibleProfile(userProfileId);
        JobPost jobPost = jobPostRepository.findById(jobPostId)
                .orElseThrow(() -> new IllegalArgumentException("岗位 ID 无效: " + jobPostId));

        ResumeReview resumeReview = resumeReviewService.analyze(userProfile, jobPost, resumeText);
        ResumeReview savedReview = resumeReviewRepository.save(resumeReview);

        fillResultModel(model, savedReview, userProfile, jobPost);
        return "resume/result";
    }

    @GetMapping("/resume-review/history")
    public String showHistory(Model model) {
        List<ResumeReview> reviews = loadCurrentUserReviews();
        model.addAttribute("reviews", reviews);
        model.addAttribute("userNameMap", buildUserNameMap());
        model.addAttribute("jobNameMap", buildJobNameMap());
        return "resume/history";
    }

    @GetMapping("/resume-review/{id}")
    public String showDetail(@PathVariable Long id, Model model) {
        ResumeReview resumeReview = resumeReviewRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("简历诊断记录不存在: " + id));
        userAccessService.assertCanAccessRecord(resumeReview.getUserAccountId(), resumeReview.getUserProfileId());

        UserProfile userProfile = userProfileRepository.findById(resumeReview.getUserProfileId()).orElse(null);
        JobPost jobPost = jobPostRepository.findById(resumeReview.getJobPostId()).orElse(null);

        fillResultModel(model, resumeReview, userProfile, jobPost);
        return "resume/result";
    }

    private void fillResultModel(Model model, ResumeReview resumeReview, UserProfile userProfile, JobPost jobPost) {
        model.addAttribute("review", resumeReview);
        model.addAttribute("userProfile", userProfile);
        model.addAttribute("jobPost", jobPost);
    }

    private Map<Long, String> buildUserNameMap() {
        Map<Long, String> userNameMap = new HashMap<>();
        for (UserProfile userProfile : userAccessService.findAccessibleProfiles()) {
            userNameMap.put(userProfile.getId(), userProfile.getStudentName());
        }
        return userNameMap;
    }

    private Map<Long, String> buildJobNameMap() {
        Map<Long, String> jobNameMap = new HashMap<>();
        for (JobPost jobPost : jobPostRepository.findAll()) {
            jobNameMap.put(jobPost.getId(), jobPost.getJobName());
        }
        return jobNameMap;
    }

    private List<ResumeReview> loadCurrentUserReviews() {
        Long userId = currentUserService.requireCurrentUserId();
        List<Long> profileIds = userAccessService.findAccessibleProfileIds();
        return Stream.concat(
                        resumeReviewRepository.findTop20ByUserAccountIdOrderByIdDesc(userId).stream(),
                        profileIds.isEmpty()
                                ? Stream.empty()
                                : resumeReviewRepository.findTop20ByUserProfileIdInAndUserAccountIdIsNullOrderByIdDesc(profileIds).stream()
                )
                .distinct()
                .limit(20)
                .toList();
    }
}
