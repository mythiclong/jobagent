package com.zhihang.jobagent.service;

import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.UpdateLog;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.UpdateLogRepository;
import com.zhihang.jobagent.service.update.SyncRunStats;
import com.zhihang.jobagent.service.update.UpdateSourceSupport;
import com.zhihang.jobagent.service.update.job.FetchedJobItem;
import com.zhihang.jobagent.service.update.job.JobSourceAdapter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class JobUpdateService {

    private final JobPostRepository jobPostRepository;
    private final UpdateLogRepository updateLogRepository;
    private final List<JobSourceAdapter> jobSourceAdapters;
    private final ContentNormalizationService contentNormalizationService;

    public JobUpdateService(JobPostRepository jobPostRepository,
                            UpdateLogRepository updateLogRepository,
                            List<JobSourceAdapter> jobSourceAdapters,
                            ContentNormalizationService contentNormalizationService) {
        this.jobPostRepository = jobPostRepository;
        this.updateLogRepository = updateLogRepository;
        this.jobSourceAdapters = jobSourceAdapters;
        this.contentNormalizationService = contentNormalizationService;
    }

    public UpdateLog runManualUpdate() {
        return runUpdate("MANUAL");
    }

    public UpdateLog runScheduledUpdate() {
        return runUpdate("SCHEDULED");
    }

    private UpdateLog runUpdate(String triggerMode) {
        List<String> activeSources = new ArrayList<>();
        int addedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        int successCount = 0;
        int failureCount = 0;

        for (JobSourceAdapter adapter : jobSourceAdapters) {
            if (!adapter.isEnabled()) {
                continue;
            }
            activeSources.add(adapter.getSourceName());
            try {
                SyncRunStats stats = syncAdapter(adapter);
                addedCount += stats.addedCount();
                updatedCount += stats.updatedCount();
                skippedCount += stats.skippedCount();
                successCount++;
                saveLog("JOB", adapter.getSourceName(), triggerMode, stats, "SUCCESS", stats.message());
            } catch (Exception ex) {
                failureCount++;
                saveLog(
                        "JOB",
                        adapter.getSourceName(),
                        triggerMode,
                        SyncRunStats.empty("Job sync failed for source " + adapter.getSourceName()),
                        "FAILED",
                        "Job sync failed: " + ex.getMessage()
                );
            }
        }

        if (activeSources.isEmpty()) {
            return saveLog(
                    "JOB",
                    "NO_SOURCE",
                    triggerMode,
                    SyncRunStats.empty("No public job sources enabled."),
                    "FAILED",
                    "No job source adapter is enabled. Enable campus/public/company whitelist sources first."
            );
        }

        String status = failureCount == 0 ? "SUCCESS" : (successCount > 0 ? "PARTIAL_SUCCESS" : "FAILED");
        String message = "Job sync finished from " + String.join(", ", activeSources)
                + ". Added " + addedCount
                + ", updated " + updatedCount
                + ", skipped " + skippedCount + ".";
        return saveLog(
                "JOB",
                String.join(", ", activeSources),
                triggerMode,
                new SyncRunStats(addedCount, updatedCount, skippedCount, message),
                status,
                message
        );
    }

    private SyncRunStats syncAdapter(JobSourceAdapter adapter) throws IOException {
        List<FetchedJobItem> items = adapter.fetchJobs();
        int addedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (FetchedJobItem item : items) {
            if (!StringUtils.hasText(item.title())) {
                skippedCount++;
                continue;
            }
            Optional<JobPost> existingByUrl = StringUtils.hasText(item.externalUrl())
                    ? jobPostRepository.findBySourceNameAndExternalUrl(item.sourceName(), item.externalUrl())
                    : Optional.empty();
            Optional<JobPost> existingByKey = StringUtils.hasText(item.externalKey())
                    ? jobPostRepository.findByExternalKey(item.externalKey())
                    : Optional.empty();

            JobPost jobPost = existingByUrl.orElseGet(() -> existingByKey.orElseGet(JobPost::new));
            boolean isNew = jobPost.getId() == null;

            jobPost.setJobName(item.title());
            jobPost.setJobDirection(defaultDirection(item.direction()));
            jobPost.setJobRequirements(defaultText(item.summary()));
            jobPost.setBonusPoints(buildBonusPoints(item));
            jobPost.setSuitableGrade(StringUtils.hasText(jobPost.getSuitableGrade()) ? jobPost.getSuitableGrade() : "不限");
            jobPost.setCompanyName(UpdateSourceSupport.truncate(defaultText(item.companyName()), 120));
            jobPost.setJobLocation(UpdateSourceSupport.truncate(defaultText(item.location()), 120));
            jobPost.setSourceName(UpdateSourceSupport.truncate(item.sourceName(), 120));
            jobPost.setExternalUrl(UpdateSourceSupport.truncate(UpdateSourceSupport.normalizeUrl(item.externalUrl()), 255));
            jobPost.setExternalKey(resolveExternalKey(item));
            jobPost.setPublishDateText(UpdateSourceSupport.truncate(defaultText(item.publishDateText()), 80));
            jobPost.setRawTitle(UpdateSourceSupport.truncate(defaultText(item.title()), 255));
            jobPost.setRawContent(UpdateSourceSupport.truncate(defaultText(item.summary()), 6000));
            jobPost.setRawUrl(UpdateSourceSupport.truncate(UpdateSourceSupport.normalizeUrl(item.externalUrl()), 255));
            jobPost.setUpdatedAt(now);
            contentNormalizationService.normalizeJob(jobPost);
            jobPostRepository.save(jobPost);

            if (isNew) {
                addedCount++;
            } else {
                updatedCount++;
            }
        }

        return new SyncRunStats(
                addedCount,
                updatedCount,
                skippedCount,
                "Fetched " + items.size() + " public job items from " + adapter.getSourceName()
        );
    }

    private String buildBonusPoints(FetchedJobItem item) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(item.companyName())) {
            parts.add("来源单位：" + item.companyName());
        }
        if (StringUtils.hasText(item.location())) {
            parts.add("工作地点：" + item.location());
        }
        if (StringUtils.hasText(item.publishDateText())) {
            parts.add("发布时间：" + item.publishDateText());
        }
        parts.add("来源：" + item.sourceName());
        return String.join(" | ", parts);
    }

    private String defaultDirection(String direction) {
        return StringUtils.hasText(direction) ? direction.trim() : "通用方向";
    }

    private String resolveExternalKey(FetchedJobItem item) {
        if (StringUtils.hasText(item.externalKey())) {
            return item.externalKey().trim();
        }
        return UpdateSourceSupport.sha256(
                item.sourceName() + "|" + item.title() + "|" + item.companyName() + "|" + item.publishDateText()
        );
    }

    private String defaultText(String value) {
        return value == null ? "" : value.trim();
    }

    private UpdateLog saveLog(String updateType,
                              String sourceName,
                              String triggerMode,
                              SyncRunStats stats,
                              String status,
                              String message) {
        UpdateLog log = new UpdateLog();
        log.setUpdateType(updateType);
        log.setSourceName(sourceName);
        log.setTriggerMode(triggerMode);
        log.setAddedCount(stats.addedCount());
        log.setUpdatedCount(stats.updatedCount());
        log.setSkippedCount(stats.skippedCount());
        log.setStatus(status);
        log.setMessage(UpdateSourceSupport.truncate(message, 240));
        log.setCreatedAt(LocalDateTime.now());
        return updateLogRepository.save(log);
    }
}
