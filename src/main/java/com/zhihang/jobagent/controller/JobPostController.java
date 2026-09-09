package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.dto.JobExcelImportPreview;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.JobSourceSnapshot;
import com.zhihang.jobagent.entity.UpdateLog;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.JobSourceSnapshotRepository;
import com.zhihang.jobagent.repository.UpdateLogRepository;
import com.zhihang.jobagent.service.ContentNormalizationService;
import com.zhihang.jobagent.service.CurrentUserService;
import com.zhihang.jobagent.service.JobExcelImportService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
public class JobPostController {

    private static final String JOB_EXCEL_PREVIEW_SESSION_KEY = "jobExcelImportPreview";
    private static final String STATE_IDLE = "idle";
    private static final String STATE_PREVIEW_READY = "preview_ready";
    private static final String STATE_IMPORTED = "imported";

    private final JobPostRepository jobPostRepository;
    private final JobSourceSnapshotRepository jobSourceSnapshotRepository;
    private final UpdateLogRepository updateLogRepository;
    private final ContentNormalizationService contentNormalizationService;
    private final JobExcelImportService jobExcelImportService;
    private final CurrentUserService currentUserService;

    public JobPostController(JobPostRepository jobPostRepository,
                             JobSourceSnapshotRepository jobSourceSnapshotRepository,
                             UpdateLogRepository updateLogRepository,
                             ContentNormalizationService contentNormalizationService,
                             JobExcelImportService jobExcelImportService,
                             CurrentUserService currentUserService) {
        this.jobPostRepository = jobPostRepository;
        this.jobSourceSnapshotRepository = jobSourceSnapshotRepository;
        this.updateLogRepository = updateLogRepository;
        this.contentNormalizationService = contentNormalizationService;
        this.jobExcelImportService = jobExcelImportService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/jobs")
    public String listJobs(Model model, HttpSession session) {
        List<JobPost> jobs = jobPostRepository.findAll().stream()
                .sorted(Comparator
                        .comparing(JobPost::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(JobPost::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        JobExcelImportPreview preview = getPreviewFromSession(session);
        UpdateLog lastJobExcelImportLog = findLastImportLog();

        model.addAttribute("jobs", jobs);
        model.addAttribute("jobCount", jobs.size());
        model.addAttribute("excelImportedJobCount", jobs.stream()
                .filter(job -> StringUtils.hasText(job.getSourceJobId()))
                .count());
        model.addAttribute("lastJobExcelImportLog", lastJobExcelImportLog);
        model.addAttribute("jobExcelImportPreview", preview);
        model.addAttribute("jobImportPageState", resolveImportPageState(preview, lastJobExcelImportLog));
        model.addAttribute("jobImportStateTitle", resolveImportStateTitle(preview, lastJobExcelImportLog));
        model.addAttribute("jobImportStateDescription", resolveImportStateDescription(preview, lastJobExcelImportLog));
        return "jobs/list";
    }

    @PostMapping("/jobs/import/preview")
    public String previewJobExcel(@RequestParam("excelFile") MultipartFile excelFile,
                                  HttpSession session,
                                  RedirectAttributes redirectAttributes) {
        try {
            JobExcelImportPreview preview = jobExcelImportService.buildPreview(excelFile);
            session.setAttribute(JOB_EXCEL_PREVIEW_SESSION_KEY, preview);
            redirectAttributes.addFlashAttribute(
                    "notice",
                    "已生成预览批次 " + preview.getImportBatchNo()
                            + "，文件 " + preview.getFileName()
                            + "，总行数 " + preview.getTotalRows()
                            + "。当前仅完成 Excel 预览，尚未写入数据库。"
            );
        } catch (IllegalArgumentException ex) {
            session.removeAttribute(JOB_EXCEL_PREVIEW_SESSION_KEY);
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/jobs";
    }

    @PostMapping("/jobs/import/confirm")
    public String confirmJobExcelImport(@RequestParam("previewToken") String previewToken,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        JobExcelImportPreview preview = getPreviewFromSession(session);
        if (preview == null) {
            redirectAttributes.addFlashAttribute("notice", "未找到可确认的 Excel 预览批次，请重新选择文件。");
            return "redirect:/jobs";
        }
        if (!StringUtils.hasText(previewToken) || !previewToken.equals(preview.getToken())) {
            redirectAttributes.addFlashAttribute("notice", "预览批次已失效，请重新选择 Excel 文件。");
            return "redirect:/jobs";
        }

        UpdateLog log = jobExcelImportService.confirmImport(preview, resolveOperatorName(session));
        session.removeAttribute(JOB_EXCEL_PREVIEW_SESSION_KEY);
        redirectAttributes.addFlashAttribute(
                "notice",
                "已完成导入批次 " + safeImportBatchNo(log)
                        + "，文件 " + safeFileName(log)
                        + "，新增 " + safeAddedCount(log)
                        + "，补充 " + safeUpdatedCount(log)
                        + "，跳过 " + safeSkippedCount(log)
                        + "，失败 " + safeFailedCount(log)
                        + "，原始快照 " + safeSnapshotCount(log)
                        + "。"
        );
        return "redirect:/jobs";
    }

    @GetMapping("/jobs/new")
    public String showCreateForm(Model model) {
        model.addAttribute("jobPost", new JobPost());
        return "jobs/form";
    }

    @PostMapping("/jobs")
    public String saveJob(@ModelAttribute JobPost formJobPost) {
        JobPost jobPost = formJobPost.getId() == null
                ? new JobPost()
                : jobPostRepository.findById(formJobPost.getId()).orElseGet(JobPost::new);

        jobPost.setJobName(formJobPost.getJobName());
        jobPost.setJobDirection(formJobPost.getJobDirection());
        jobPost.setJobRequirements(formJobPost.getJobRequirements());
        jobPost.setBonusPoints(formJobPost.getBonusPoints());
        jobPost.setSuitableGrade(formJobPost.getSuitableGrade());
        jobPost.setRawTitle(formJobPost.getJobName());
        jobPost.setRawContent((formJobPost.getJobRequirements() == null ? "" : formJobPost.getJobRequirements())
                + "\n"
                + (formJobPost.getBonusPoints() == null ? "" : formJobPost.getBonusPoints()));
        contentNormalizationService.normalizeJob(jobPost);
        jobPostRepository.save(jobPost);
        return "redirect:/jobs";
    }

    @GetMapping("/jobs/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        JobPost jobPost = jobPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("岗位ID无效: " + id));
        model.addAttribute("jobPost", jobPost);
        return "jobs/form";
    }

    @GetMapping("/jobs/{id}")
    public String showJobDetail(@PathVariable Long id, Model model) {
        JobPost jobPost = jobPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("岗位ID无效: " + id));
        JobSourceSnapshot latestSnapshot = jobSourceSnapshotRepository
                .findTopByJobPostOrderByCreatedAtDescIdDesc(jobPost)
                .orElse(null);

        model.addAttribute("jobPost", jobPost);
        model.addAttribute("latestSourceSnapshot", latestSnapshot);
        model.addAttribute("recentSourceSnapshots", jobSourceSnapshotRepository.findTop5ByJobPostOrderByCreatedAtDescIdDesc(jobPost));
        return "jobs/detail";
    }

    @GetMapping("/jobs/delete/{id}")
    public String deleteJob(@PathVariable Long id) {
        JobPost jobPost = jobPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("岗位ID无效: " + id));
        jobSourceSnapshotRepository.deleteByJobPost(jobPost);
        jobPostRepository.delete(jobPost);
        return "redirect:/jobs";
    }

    private UpdateLog findLastImportLog() {
        return updateLogRepository.findTopByUpdateTypeOrderByCreatedAtDescIdDesc(JobExcelImportService.UPDATE_TYPE).orElse(null);
    }

    private JobExcelImportPreview getPreviewFromSession(HttpSession session) {
        Object preview = session.getAttribute(JOB_EXCEL_PREVIEW_SESSION_KEY);
        if (preview instanceof JobExcelImportPreview jobExcelImportPreview) {
            return jobExcelImportPreview;
        }
        return null;
    }

    private String resolveImportPageState(JobExcelImportPreview preview, UpdateLog log) {
        if (preview != null) {
            return STATE_PREVIEW_READY;
        }
        if (log != null) {
            return STATE_IMPORTED;
        }
        return STATE_IDLE;
    }

    private String resolveImportStateTitle(JobExcelImportPreview preview, UpdateLog log) {
        if (preview != null) {
            return "当前状态：已生成预览批次，尚未执行导入";
        }
        if (log != null) {
            return "当前状态：最近一次确认导入已完成";
        }
        return "当前状态：未上传文件";
    }

    private String resolveImportStateDescription(JobExcelImportPreview preview, UpdateLog log) {
        if (preview != null) {
            return "当前仅完成 Excel 预览，尚未执行导入。下方“已导入数据概览”和“最近导入记录”只统计已确认导入成功的数据，不包含当前预览批次。";
        }
        if (log != null) {
            return "当前没有待确认的预览批次，页面展示的导入统计和最近导入记录均来自已经确认写库成功的结果。";
        }
        return "当前还没有预览批次和导入记录。请先选择 Excel 文件生成预览，再决定是否执行导入。";
    }

    private String resolveOperatorName(HttpSession session) {
        if (currentUserService.isAuthenticated()) {
            return currentUserService.requireCurrentUserAccount().getUsername();
        }
        return "admin";
    }

    private String safeFileName(UpdateLog log) {
        return log == null || !StringUtils.hasText(log.getFileName()) ? "-" : log.getFileName();
    }

    private String safeImportBatchNo(UpdateLog log) {
        return log == null || !StringUtils.hasText(log.getImportBatchNo()) ? "-" : log.getImportBatchNo();
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

    private int safeFailedCount(UpdateLog log) {
        return log == null || log.getFailedCount() == null ? 0 : log.getFailedCount();
    }

    private int safeSnapshotCount(UpdateLog log) {
        return log == null || log.getSnapshotCount() == null ? 0 : log.getSnapshotCount();
    }
}
