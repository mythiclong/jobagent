package com.zhihang.jobagent.service;

import com.zhihang.jobagent.entity.InterviewQuestion;
import com.zhihang.jobagent.entity.UpdateLog;
import com.zhihang.jobagent.repository.InterviewQuestionRepository;
import com.zhihang.jobagent.repository.UpdateLogRepository;
import com.zhihang.jobagent.service.update.SyncRunStats;
import com.zhihang.jobagent.service.update.UpdateSourceSupport;
import com.zhihang.jobagent.service.update.question.FetchedInterviewQuestionItem;
import com.zhihang.jobagent.service.update.question.InterviewQuestionSourceAdapter;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class InterviewQuestionUpdateService {

    private final InterviewQuestionRepository interviewQuestionRepository;
    private final UpdateLogRepository updateLogRepository;
    private final List<InterviewQuestionSourceAdapter> sourceAdapters;
    private final ContentNormalizationService contentNormalizationService;
    private final InterviewQuestionTranslationService interviewQuestionTranslationService;

    public InterviewQuestionUpdateService(InterviewQuestionRepository interviewQuestionRepository,
                                          UpdateLogRepository updateLogRepository,
                                          List<InterviewQuestionSourceAdapter> sourceAdapters,
                                          ContentNormalizationService contentNormalizationService,
                                          InterviewQuestionTranslationService interviewQuestionTranslationService) {
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.updateLogRepository = updateLogRepository;
        this.sourceAdapters = sourceAdapters;
        this.contentNormalizationService = contentNormalizationService;
        this.interviewQuestionTranslationService = interviewQuestionTranslationService;
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

        for (InterviewQuestionSourceAdapter adapter : sourceAdapters) {
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
                saveLog("QUESTION", adapter.getSourceName(), triggerMode, stats, "SUCCESS", stats.message());
            } catch (Exception ex) {
                failureCount++;
                saveLog(
                        "QUESTION",
                        adapter.getSourceName(),
                        triggerMode,
                        SyncRunStats.empty("Interview question sync failed for source " + adapter.getSourceName()),
                        "FAILED",
                        "Interview question sync failed: " + ex.getMessage()
                );
            }
        }

        if (activeSources.isEmpty()) {
            return saveLog(
                    "QUESTION",
                    "NO_SOURCE",
                    triggerMode,
                    SyncRunStats.empty("No interview question sources enabled."),
                    "FAILED",
                    "No interview question source adapter is enabled. Enable GitHub or whitelist article sources first."
            );
        }

        String status = failureCount == 0 ? "SUCCESS" : (successCount > 0 ? "PARTIAL_SUCCESS" : "FAILED");
        String message = "Interview question sync finished from " + String.join(", ", activeSources)
                + ". Added " + addedCount
                + ", updated " + updatedCount
                + ", skipped " + skippedCount + ".";
        return saveLog(
                "QUESTION",
                String.join(", ", activeSources),
                triggerMode,
                new SyncRunStats(addedCount, updatedCount, skippedCount, message),
                status,
                message
        );
    }

    private SyncRunStats syncAdapter(InterviewQuestionSourceAdapter adapter) throws IOException {
        List<FetchedInterviewQuestionItem> items = adapter.fetchQuestions();
        int addedCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        LocalDateTime now = LocalDateTime.now();

        List<InterviewQuestion> questionsToPersist = new ArrayList<>();
        for (FetchedInterviewQuestionItem item : items) {
            if (!StringUtils.hasText(item.questionText()) || !StringUtils.hasText(item.questionHash())) {
                skippedCount++;
                continue;
            }

            InterviewQuestion question = interviewQuestionRepository.findByQuestionHash(item.questionHash())
                    .orElseGet(InterviewQuestion::new);
            boolean isNew = question.getId() == null;

            question.setJobDirection(mapDirection(item.topicTag()));
            question.setQuestionType(item.topicTag());
            question.setQuestionText(item.questionTitle() != null ? item.questionTitle() : item.questionText());
            question.setReferenceAnswer(item.sourceSummary());
            question.setKeyPoints(item.topicTag());
            question.setSourceName(UpdateSourceSupport.truncate(item.sourceName(), 120));
            question.setSourceUrl(UpdateSourceSupport.truncate(item.sourceUrl(), 255));
            question.setQuestionHash(item.questionHash());
            question.setSourceSummary(item.sourceSummary());
            question.setRawTitle(UpdateSourceSupport.truncate(defaultText(item.questionTitle(), item.questionText()), 255));
            question.setRawContent(UpdateSourceSupport.truncate(defaultText(item.questionText(), item.sourceSummary()), 6000));
            question.setRawUrl(UpdateSourceSupport.truncate(item.sourceUrl(), 255));
            question.setUpdatedAt(now);
            questionsToPersist.add(question);
            if (isNew) {
                addedCount++;
            } else {
                updatedCount++;
            }
        }

        interviewQuestionTranslationService.translateTitlesIfNeeded(questionsToPersist);
        for (InterviewQuestion question : questionsToPersist) {
            contentNormalizationService.normalizeInterviewQuestion(question);
            interviewQuestionRepository.save(question);
        }

        return new SyncRunStats(
                addedCount,
                updatedCount,
                skippedCount,
                "Fetched " + items.size() + " interview questions from " + adapter.getSourceName()
        );
    }

    private String mapDirection(String topicTag) {
        String tag = topicTag == null ? "" : topicTag.toLowerCase();
        if (tag.contains("react") || tag.contains("javascript")) {
            return "前端开发";
        }
        if (tag.contains("product") || tag.contains("pm")) {
            return "产品经理";
        }
        return "Java后端";
    }

    private String defaultText(String primary, String secondary) {
        if (StringUtils.hasText(primary)) {
            return primary.trim();
        }
        return secondary == null ? "" : secondary.trim();
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
