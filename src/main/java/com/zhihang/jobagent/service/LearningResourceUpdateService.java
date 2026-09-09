package com.zhihang.jobagent.service;

import com.zhihang.jobagent.entity.LearningResource;
import com.zhihang.jobagent.entity.UpdateLog;
import com.zhihang.jobagent.repository.LearningResourceRepository;
import com.zhihang.jobagent.repository.UpdateLogRepository;
import com.zhihang.jobagent.service.update.SyncRunStats;
import com.zhihang.jobagent.service.update.UpdateSourceSupport;
import com.zhihang.jobagent.service.update.resource.FetchedLearningResourceItem;
import com.zhihang.jobagent.service.update.resource.LearningResourceSourceAdapter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class LearningResourceUpdateService {

    private final LearningResourceRepository learningResourceRepository;
    private final UpdateLogRepository updateLogRepository;
    private final List<LearningResourceSourceAdapter> sourceAdapters;
    private final ContentNormalizationService contentNormalizationService;

    public LearningResourceUpdateService(LearningResourceRepository learningResourceRepository,
                                         UpdateLogRepository updateLogRepository,
                                         List<LearningResourceSourceAdapter> sourceAdapters,
                                         ContentNormalizationService contentNormalizationService) {
        this.learningResourceRepository = learningResourceRepository;
        this.updateLogRepository = updateLogRepository;
        this.sourceAdapters = sourceAdapters;
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

        for (LearningResourceSourceAdapter adapter : sourceAdapters) {
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
                saveLog("RESOURCE", adapter.getSourceName(), triggerMode, stats, "SUCCESS", stats.message());
            } catch (Exception ex) {
                failureCount++;
                saveLog(
                        "RESOURCE",
                        adapter.getSourceName(),
                        triggerMode,
                        SyncRunStats.empty("Learning resource sync failed for source " + adapter.getSourceName()),
                        "FAILED",
                        "Learning resource sync failed: " + ex.getMessage()
                );
            }
        }

        if (activeSources.isEmpty()) {
            return saveLog(
                    "RESOURCE",
                    "NO_SOURCE",
                    triggerMode,
                    SyncRunStats.empty("No learning resource sources enabled."),
                    "FAILED",
                    "No learning resource source adapter is enabled. Enable official or whitelist tutorial sources first."
            );
        }

        String status = failureCount == 0 ? "SUCCESS" : (successCount > 0 ? "PARTIAL_SUCCESS" : "FAILED");
        String message = "Learning resource sync finished from " + String.join(", ", activeSources)
                + ". Added " + addedCount
                + ", updated " + updatedCount
                + ", skipped " + skippedCount + ".";
        return saveLog(
                "RESOURCE",
                String.join(", ", activeSources),
                triggerMode,
                new SyncRunStats(addedCount, updatedCount, skippedCount, message),
                status,
                message
        );
    }

    private SyncRunStats syncAdapter(LearningResourceSourceAdapter adapter) throws IOException {
        List<FetchedLearningResourceItem> items = adapter.fetchResources();
        int addedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        for (FetchedLearningResourceItem item : items) {
            if (!StringUtils.hasText(item.title()) || !StringUtils.hasText(item.url())) {
                skippedCount++;
                continue;
            }
            String normalizedUrl = UpdateSourceSupport.normalizeUrl(item.url());
            Optional<LearningResource> existingByUrl = learningResourceRepository.findByUrl(normalizedUrl);
            Optional<LearningResource> existingByTitle = learningResourceRepository
                    .findByTitleAndTargetDirection(item.title(), item.targetDirection());

            LearningResource resource = existingByUrl.orElseGet(() -> existingByTitle.orElseGet(LearningResource::new));
            boolean isNew = resource.getId() == null;

            resource.setTitle(item.title());
            resource.setUrl(UpdateSourceSupport.truncate(normalizedUrl, 255));
            resource.setResourceType(item.resourceType());
            resource.setTargetDirection(item.targetDirection());
            resource.setStageTag(item.stageTag());
            resource.setSummary(item.summary());
            resource.setSource(UpdateSourceSupport.truncate(item.sourceName(), 120));
            resource.setRawTitle(UpdateSourceSupport.truncate(item.title(), 255));
            resource.setRawContent(UpdateSourceSupport.truncate(defaultText(item.summary()), 4000));
            resource.setRawUrl(UpdateSourceSupport.truncate(normalizedUrl, 255));
            resource.setUpdatedAt(now);
            contentNormalizationService.normalizeLearningResource(resource);
            learningResourceRepository.save(resource);

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
                "Fetched " + items.size() + " learning resources from " + adapter.getSourceName()
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
