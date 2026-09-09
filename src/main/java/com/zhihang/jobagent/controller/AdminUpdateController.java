package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.entity.UpdateLog;
import com.zhihang.jobagent.repository.InterviewQuestionRepository;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.LearningResourceRepository;
import com.zhihang.jobagent.repository.UpdateLogRepository;
import com.zhihang.jobagent.service.InterviewQuestionUpdateService;
import com.zhihang.jobagent.service.JobUpdateService;
import com.zhihang.jobagent.service.LearningResourceUpdateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminUpdateController {

    private final JobUpdateService jobUpdateService;
    private final InterviewQuestionUpdateService interviewQuestionUpdateService;
    private final LearningResourceUpdateService learningResourceUpdateService;
    private final UpdateLogRepository updateLogRepository;
    private final JobPostRepository jobPostRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final LearningResourceRepository learningResourceRepository;
    private final boolean scheduleEnabled;
    private final String scheduleCron;

    public AdminUpdateController(JobUpdateService jobUpdateService,
                                 InterviewQuestionUpdateService interviewQuestionUpdateService,
                                 LearningResourceUpdateService learningResourceUpdateService,
                                 UpdateLogRepository updateLogRepository,
                                 JobPostRepository jobPostRepository,
                                 InterviewQuestionRepository interviewQuestionRepository,
                                 LearningResourceRepository learningResourceRepository,
                                 @Value("${jobagent.update-center.schedule-enabled:false}") boolean scheduleEnabled,
                                 @Value("${jobagent.update-center.schedule-cron:0 0 3 * * *}") String scheduleCron) {
        this.jobUpdateService = jobUpdateService;
        this.interviewQuestionUpdateService = interviewQuestionUpdateService;
        this.learningResourceUpdateService = learningResourceUpdateService;
        this.updateLogRepository = updateLogRepository;
        this.jobPostRepository = jobPostRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.learningResourceRepository = learningResourceRepository;
        this.scheduleEnabled = scheduleEnabled;
        this.scheduleCron = scheduleCron;
    }

    @GetMapping("/admin/update-center")
    public String showUpdateCenter(Model model) {
        populateUpdateCenterModel(model);
        return "admin/update-center";
    }

    @PostMapping("/admin/update-center/jobs")
    public String updateJobs(RedirectAttributes redirectAttributes) {
        UpdateLog log = jobUpdateService.runManualUpdate();
        addSyncNotice(redirectAttributes, "岗位来源更新", log);
        return "redirect:/admin/update-center";
    }

    @PostMapping("/admin/update-center/questions")
    public String updateQuestions(RedirectAttributes redirectAttributes) {
        UpdateLog log = interviewQuestionUpdateService.runManualUpdate();
        addSyncNotice(redirectAttributes, "面试题更新", log);
        return "redirect:/admin/update-center";
    }

    @PostMapping("/admin/update-center/resources")
    public String updateResources(RedirectAttributes redirectAttributes) {
        UpdateLog log = learningResourceUpdateService.runManualUpdate();
        addSyncNotice(redirectAttributes, "学习资源更新", log);
        return "redirect:/admin/update-center";
    }

    @PostMapping("/admin/update-center/all")
    public String updateAll(RedirectAttributes redirectAttributes) {
        UpdateLog jobLog = jobUpdateService.runManualUpdate();
        UpdateLog questionLog = interviewQuestionUpdateService.runManualUpdate();
        UpdateLog resourceLog = learningResourceUpdateService.runManualUpdate();

        String notice = "批量更新完成：岗位新增 " + safeAddedCount(jobLog)
                + "，面试题新增 " + safeAddedCount(questionLog)
                + "，学习资源新增 " + safeAddedCount(resourceLog) + "。";
        redirectAttributes.addFlashAttribute("notice", notice);
        return "redirect:/admin/update-center";
    }

    private void populateUpdateCenterModel(Model model) {
        model.addAttribute("lastJobUpdateLog", findLastLog("JOB"));
        model.addAttribute("lastQuestionUpdateLog", findLastLog("QUESTION"));
        model.addAttribute("lastResourceUpdateLog", findLastLog("RESOURCE"));
        model.addAttribute("jobUpdateLogs", updateLogRepository.findTop10ByUpdateTypeOrderByCreatedAtDescIdDesc("JOB"));
        model.addAttribute("questionUpdateLogs", updateLogRepository.findTop10ByUpdateTypeOrderByCreatedAtDescIdDesc("QUESTION"));
        model.addAttribute("resourceUpdateLogs", updateLogRepository.findTop10ByUpdateTypeOrderByCreatedAtDescIdDesc("RESOURCE"));
        model.addAttribute("recentUpdateLogs", updateLogRepository.findTop20ByOrderByCreatedAtDescIdDesc());
        model.addAttribute("jobCount", jobPostRepository.count());
        model.addAttribute("questionCount", interviewQuestionRepository.count());
        model.addAttribute("resourceCount", learningResourceRepository.count());
        model.addAttribute("latestResources", learningResourceRepository.findTop20ByOrderByUpdatedAtDescIdDesc().stream().limit(6).toList());
        model.addAttribute("scheduleEnabled", scheduleEnabled);
        model.addAttribute("scheduleCron", scheduleCron);
    }

    private UpdateLog findLastLog(String updateType) {
        return updateLogRepository.findTopByUpdateTypeOrderByCreatedAtDescIdDesc(updateType).orElse(null);
    }

    private void addSyncNotice(RedirectAttributes redirectAttributes, String moduleName, UpdateLog log) {
        if (log == null) {
            redirectAttributes.addFlashAttribute("notice", moduleName + " 请求已提交。");
            return;
        }
        String status = log.getStatus() == null ? "UNKNOWN" : log.getStatus();
        if ("SUCCESS".equalsIgnoreCase(status) || "PARTIAL_SUCCESS".equalsIgnoreCase(status)) {
            redirectAttributes.addFlashAttribute(
                    "notice",
                    moduleName + "完成：新增 " + safeAddedCount(log)
                            + "，更新 " + safeUpdatedCount(log)
                            + "，跳过 " + safeSkippedCount(log)
                            + "。来源：" + safeSourceName(log)
            );
            return;
        }
        redirectAttributes.addFlashAttribute(
                "notice",
                moduleName + "失败：" + (log.getMessage() == null ? "请查看更新日志。" : log.getMessage())
        );
    }

    private int safeAddedCount(UpdateLog log) {
        return log == null || log.getAddedCount() == null ? 0 : log.getAddedCount();
    }

    private int safeUpdatedCount(UpdateLog log) {
        return log == null || log.getUpdatedCount() == null ? 0 : log.getUpdatedCount();
    }

    private int safeSkippedCount(UpdateLog log) {
        return log == null || log.getSkippedCount() == null ? 0 : log.getSkippedCount();
    }

    private String safeSourceName(UpdateLog log) {
        return log == null || !StringUtils.hasText(log.getSourceName()) ? "-" : log.getSourceName();
    }
}
