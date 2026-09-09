package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.dto.InterviewSessionSummary;
import com.zhihang.jobagent.dto.InterviewQuestionSelectionResult;
import com.zhihang.jobagent.entity.InterviewQuestion;
import com.zhihang.jobagent.entity.InterviewRecord;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.InterviewQuestionRepository;
import com.zhihang.jobagent.repository.InterviewRecordRepository;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.UserProfileRepository;
import com.zhihang.jobagent.service.CurrentUserService;
import com.zhihang.jobagent.service.InterviewService;
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
public class InterviewController {

    private final UserProfileRepository userProfileRepository;
    private final JobPostRepository jobPostRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final InterviewRecordRepository interviewRecordRepository;
    private final InterviewService interviewService;
    private final UserAccessService userAccessService;
    private final CurrentUserService currentUserService;

    public InterviewController(UserProfileRepository userProfileRepository,
                               JobPostRepository jobPostRepository,
                               InterviewQuestionRepository interviewQuestionRepository,
                               InterviewRecordRepository interviewRecordRepository,
                               InterviewService interviewService,
                               UserAccessService userAccessService,
                               CurrentUserService currentUserService) {
        this.userProfileRepository = userProfileRepository;
        this.jobPostRepository = jobPostRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.interviewRecordRepository = interviewRecordRepository;
        this.interviewService = interviewService;
        this.userAccessService = userAccessService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/interview")
    public String showStartPage(@RequestParam(required = false) Long profileId,
                                @RequestParam(required = false) Long userProfileId,
                                @RequestParam(required = false) Long jobId,
                                @RequestParam(required = false) Long jobPostId,
                                @RequestParam(required = false) String source,
                                Model model) {
        Long selectedProfileId = userProfileId != null ? userProfileId : profileId;
        Long selectedJobId = jobPostId != null ? jobPostId : jobId;
        model.addAttribute("profiles", userAccessService.findAccessibleProfiles());
        model.addAttribute("jobs", jobPostRepository.findAll());
        model.addAttribute("selectedProfileId", selectedProfileId);
        model.addAttribute("selectedJobId", selectedJobId);
        model.addAttribute("source", source);
        return "interview/start";
    }

    @PostMapping("/interview/start")
    public String startInterview(@RequestParam Long userProfileId,
                                 @RequestParam Long jobPostId,
                                 Model model) {
        UserProfile userProfile = userAccessService.requireAccessibleProfile(userProfileId);
        JobPost jobPost = jobPostRepository.findById(jobPostId)
                .orElseThrow(() -> new IllegalArgumentException("岗位 ID 无效: " + jobPostId));

        InterviewQuestionSelectionResult selectionResult = interviewService.selectQuestions(userProfile, jobPost);

        model.addAttribute("userProfile", userProfile);
        model.addAttribute("jobPost", jobPost);
        model.addAttribute("questions", selectionResult.getQuestions());
        model.addAttribute("questionSupplyNotice", selectionResult.getNoticeMessage());
        model.addAttribute("relevantQuestionCount", selectionResult.getRelevantQuestionCount());
        model.addAttribute("generatedQuestionCount", selectionResult.getGeneratedQuestionCount());
        model.addAttribute("sessionId", interviewService.createSessionId());
        return "interview/session";
    }

    @PostMapping("/interview/submit")
    public String submitInterview(@RequestParam Long userProfileId,
                                  @RequestParam Long jobPostId,
                                  @RequestParam String sessionId,
                                  @RequestParam List<Long> questionIds,
                                  @RequestParam List<String> userAnswers,
                                  Model model) {
        UserProfile userProfile = userAccessService.requireAccessibleProfile(userProfileId);
        JobPost jobPost = jobPostRepository.findById(jobPostId)
                .orElseThrow(() -> new IllegalArgumentException("岗位 ID 无效: " + jobPostId));

        List<InterviewRecord> records = interviewService.saveInterviewRecords(
                sessionId, userProfile, jobPost, questionIds, userAnswers
        );

        fillSessionResultModel(model, sessionId, userProfile, jobPost, records);
        return "interview/result";
    }

    @GetMapping("/interview/history")
    public String showHistory(Model model) {
        List<InterviewRecord> allRecords = loadCurrentUserInterviewRecords();
        List<InterviewSessionSummary> sessions = interviewService.buildSessionSummaries(allRecords);

        model.addAttribute("sessions", sessions);
        model.addAttribute("userNameMap", buildUserNameMap());
        model.addAttribute("jobNameMap", buildJobNameMap());
        return "interview/history";
    }

    @GetMapping("/interview/history/{sessionId}")
    public String showSessionDetail(@PathVariable String sessionId, Model model) {
        List<InterviewRecord> records = interviewService.findSessionRecords(sessionId);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("面试记录不存在: " + sessionId);
        }
        InterviewRecord firstRecord = records.get(0);
        userAccessService.assertCanAccessRecord(firstRecord.getUserAccountId(), firstRecord.getUserProfileId());

        UserProfile userProfile = userProfileRepository.findById(firstRecord.getUserProfileId()).orElse(null);
        JobPost jobPost = jobPostRepository.findById(firstRecord.getJobPostId()).orElse(null);

        fillSessionResultModel(model, sessionId, userProfile, jobPost, records);
        return "interview/detail";
    }

    private void fillSessionResultModel(Model model, String sessionId, UserProfile userProfile,
                                        JobPost jobPost, List<InterviewRecord> records) {
        model.addAttribute("sessionId", sessionId);
        model.addAttribute("userProfile", userProfile);
        model.addAttribute("jobPost", jobPost);
        model.addAttribute("records", records);
        model.addAttribute("questionMap", buildQuestionMap(records));
        model.addAttribute("totalScore", interviewService.calculateTotalScore(records));
        model.addAttribute("overallComment", interviewService.buildOverallComment(records));
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

    private Map<Long, InterviewQuestion> buildQuestionMap(List<InterviewRecord> records) {
        Map<Long, InterviewQuestion> questionMap = new HashMap<>();
        List<Long> ids = records.stream()
                .map(InterviewRecord::getQuestionId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return questionMap;
        }
        for (InterviewQuestion question : interviewQuestionRepository.findAllById(ids)) {
            questionMap.put(question.getId(), question);
        }
        return questionMap;
    }

    private List<InterviewRecord> loadCurrentUserInterviewRecords() {
        Long userId = currentUserService.requireCurrentUserId();
        List<Long> profileIds = userAccessService.findAccessibleProfileIds();
        return Stream.concat(
                        interviewRecordRepository.findTop50ByUserAccountIdOrderByCreatedAtDescIdDesc(userId).stream(),
                        profileIds.isEmpty()
                                ? Stream.empty()
                                : interviewRecordRepository.findTop50ByUserProfileIdInAndUserAccountIdIsNullOrderByCreatedAtDescIdDesc(profileIds).stream()
                )
                .distinct()
                .limit(50)
                .toList();
    }
}
