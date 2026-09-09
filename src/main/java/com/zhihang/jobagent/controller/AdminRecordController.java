package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.dto.InterviewSessionSummary;
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
import com.zhihang.jobagent.repository.ResumeReviewRepository;
import com.zhihang.jobagent.repository.UserProfileRepository;
import com.zhihang.jobagent.service.InterviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Controller
public class AdminRecordController {

    private static final Logger log = LoggerFactory.getLogger(AdminRecordController.class);

    private final UserProfileRepository userProfileRepository;
    private final JobPostRepository jobPostRepository;
    private final JobMatchRecordRepository jobMatchRecordRepository;
    private final ResumeReviewRepository resumeReviewRepository;
    private final ResumeEnhanceRecordRepository resumeEnhanceRecordRepository;
    private final InterviewRecordRepository interviewRecordRepository;
    private final LearningPathAdviceRepository learningPathAdviceRepository;
    private final InterviewService interviewService;

    public AdminRecordController(UserProfileRepository userProfileRepository,
                                 JobPostRepository jobPostRepository,
                                 JobMatchRecordRepository jobMatchRecordRepository,
                                 ResumeReviewRepository resumeReviewRepository,
                                 ResumeEnhanceRecordRepository resumeEnhanceRecordRepository,
                                 InterviewRecordRepository interviewRecordRepository,
                                 LearningPathAdviceRepository learningPathAdviceRepository,
                                 InterviewService interviewService) {
        this.userProfileRepository = userProfileRepository;
        this.jobPostRepository = jobPostRepository;
        this.jobMatchRecordRepository = jobMatchRecordRepository;
        this.resumeReviewRepository = resumeReviewRepository;
        this.resumeEnhanceRecordRepository = resumeEnhanceRecordRepository;
        this.interviewRecordRepository = interviewRecordRepository;
        this.learningPathAdviceRepository = learningPathAdviceRepository;
        this.interviewService = interviewService;
    }

    @GetMapping({"/admin/user-records", "/admin/user-records/"})
    public String showUserRecords(Model model) {
        List<JobMatchRecord> matchRecords = safeList(
                () -> jobMatchRecordRepository.findTop10ByOrderByCreatedAtDescIdDesc(),
                "读取岗位匹配记录失败"
        );
        List<ResumeReview> resumeReviews = safeList(
                () -> resumeReviewRepository.findTop10ByOrderByIdDesc(),
                "读取简历诊断记录失败"
        );
        List<InterviewRecord> interviewRecords = safeList(
                () -> interviewRecordRepository.findTop30ByOrderByCreatedAtDescIdDesc(),
                "读取模拟面试记录失败"
        );
        List<ResumeEnhanceRecord> resumeEnhanceRecords = safeList(
                () -> resumeEnhanceRecordRepository.findTop10ByOrderByCreatedAtDescIdDesc(),
                "读取简历增强记录失败"
        );
        List<InterviewSessionSummary> interviewSessions = safeList(
                () -> interviewService.buildSessionSummaries(interviewRecords),
                "构建面试 session 汇总失败"
        );
        List<LearningPathAdvice> learningPathRecords = safeList(
                () -> learningPathAdviceRepository.findTop10ByOrderByCreatedAtDescIdDesc(),
                "读取学习路径建议记录失败"
        );

        model.addAttribute("jobCount", safeCount(() -> jobPostRepository.count(), "读取岗位总数失败"));
        model.addAttribute("profileCount", safeCount(() -> userProfileRepository.count(), "读取画像总数失败"));
        model.addAttribute("matchCount", safeCount(() -> jobMatchRecordRepository.count(), "读取匹配记录总数失败"));
        model.addAttribute("resumeReviewCount", safeCount(() -> resumeReviewRepository.count(), "读取简历诊断总数失败"));
        model.addAttribute("resumeEnhanceCount", safeCount(() -> resumeEnhanceRecordRepository.count(), "读取简历增强总数失败"));
        model.addAttribute("interviewRecordCount", safeCount(() -> interviewRecordRepository.count(), "读取面试记录总数失败"));
        model.addAttribute("learningPathCount", safeCount(() -> learningPathAdviceRepository.count(), "读取学习路径建议总数失败"));

        model.addAttribute("matchRecords", matchRecords);
        model.addAttribute("resumeReviews", resumeReviews);
        model.addAttribute("resumeEnhanceRecords", resumeEnhanceRecords);
        model.addAttribute("interviewSessions", interviewSessions.stream().limit(8).toList());
        model.addAttribute("learningPathRecords", learningPathRecords);
        model.addAttribute("userNameMap", buildUserNameMap());
        model.addAttribute("jobNameMap", buildJobNameMap());
        return "admin/user-records";
    }

    @GetMapping("/admin/users")
    public String redirectLegacyUsersPage() {
        return "redirect:/admin/user-records";
    }

    private Map<Long, String> buildUserNameMap() {
        Map<Long, String> map = new HashMap<>();
        try {
            for (UserProfile profile : userProfileRepository.findAll()) {
                map.put(profile.getId(), profile.getStudentName());
            }
        } catch (Exception ex) {
            log.error("读取用户画像映射失败", ex);
        }
        return map;
    }

    private Map<Long, String> buildJobNameMap() {
        Map<Long, String> map = new HashMap<>();
        try {
            for (JobPost jobPost : jobPostRepository.findAll()) {
                map.put(jobPost.getId(), jobPost.getJobName());
            }
        } catch (Exception ex) {
            log.error("读取岗位映射失败", ex);
        }
        return map;
    }

    private <T> List<T> safeList(Supplier<List<T>> supplier, String message) {
        try {
            List<T> values = supplier.get();
            return values == null ? Collections.emptyList() : values;
        } catch (Exception ex) {
            log.error(message, ex);
            return Collections.emptyList();
        }
    }

    private long safeCount(Supplier<Long> supplier, String message) {
        try {
            Long value = supplier.get();
            return value == null ? 0L : value;
        } catch (Exception ex) {
            log.error(message, ex);
            return 0L;
        }
    }
}
