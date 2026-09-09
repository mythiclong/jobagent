package com.zhihang.jobagent.service;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

@Service
public class HistoryCenterService {

    private static final Logger log = LoggerFactory.getLogger(HistoryCenterService.class);

    private final JobMatchRecordRepository jobMatchRecordRepository;
    private final ResumeReviewRepository resumeReviewRepository;
    private final ResumeEnhanceRecordRepository resumeEnhanceRecordRepository;
    private final InterviewRecordRepository interviewRecordRepository;
    private final LearningPathAdviceRepository learningPathAdviceRepository;
    private final UserProfileRepository userProfileRepository;
    private final JobPostRepository jobPostRepository;
    private final InterviewService interviewService;
    private final UserAccessService userAccessService;
    private final CurrentUserService currentUserService;

    public HistoryCenterService(JobMatchRecordRepository jobMatchRecordRepository,
                                ResumeReviewRepository resumeReviewRepository,
                                ResumeEnhanceRecordRepository resumeEnhanceRecordRepository,
                                InterviewRecordRepository interviewRecordRepository,
                                LearningPathAdviceRepository learningPathAdviceRepository,
                                UserProfileRepository userProfileRepository,
                                JobPostRepository jobPostRepository,
                                InterviewService interviewService,
                                UserAccessService userAccessService,
                                CurrentUserService currentUserService) {
        this.jobMatchRecordRepository = jobMatchRecordRepository;
        this.resumeReviewRepository = resumeReviewRepository;
        this.resumeEnhanceRecordRepository = resumeEnhanceRecordRepository;
        this.interviewRecordRepository = interviewRecordRepository;
        this.learningPathAdviceRepository = learningPathAdviceRepository;
        this.userProfileRepository = userProfileRepository;
        this.jobPostRepository = jobPostRepository;
        this.interviewService = interviewService;
        this.userAccessService = userAccessService;
        this.currentUserService = currentUserService;
    }

    public List<JobMatchRecord> loadMatchRecords() {
        Long userId = currentUserService.requireCurrentUserId();
        List<Long> profileIds = userAccessService.findAccessibleProfileIds();
        return mergeRecords(
                safeList(() -> jobMatchRecordRepository.findTop20ByUserAccountIdOrderByCreatedAtDescIdDesc(userId),
                        "Failed to load job match records."),
                profileIds.isEmpty()
                        ? List.of()
                        : safeList(() -> jobMatchRecordRepository.findTop20ByUserProfileIdInAndUserAccountIdIsNullOrderByCreatedAtDescIdDesc(profileIds),
                        "Failed to load legacy job match records.")
        ).stream().limit(10).toList();
    }

    public List<ResumeReview> loadResumeReviews() {
        Long userId = currentUserService.requireCurrentUserId();
        List<Long> profileIds = userAccessService.findAccessibleProfileIds();
        return mergeRecords(
                safeList(() -> resumeReviewRepository.findTop20ByUserAccountIdOrderByIdDesc(userId),
                        "Failed to load resume review records."),
                profileIds.isEmpty()
                        ? List.of()
                        : safeList(() -> resumeReviewRepository.findTop20ByUserProfileIdInAndUserAccountIdIsNullOrderByIdDesc(profileIds),
                        "Failed to load legacy resume review records.")
        ).stream().limit(10).toList();
    }

    public List<ResumeEnhanceRecord> loadResumeEnhanceRecords() {
        Long userId = currentUserService.requireCurrentUserId();
        List<Long> profileIds = userAccessService.findAccessibleProfileIds();
        return mergeRecords(
                safeList(() -> resumeEnhanceRecordRepository.findTop20ByUserAccountIdOrderByCreatedAtDescIdDesc(userId),
                        "Failed to load resume optimize records."),
                profileIds.isEmpty()
                        ? List.of()
                        : safeList(() -> resumeEnhanceRecordRepository.findTop20ByUserProfileIdInAndUserAccountIdIsNullOrderByCreatedAtDescIdDesc(profileIds),
                        "Failed to load legacy resume optimize records.")
        ).stream().limit(10).toList();
    }

    public List<LearningPathAdvice> loadStrengthPlanRecords() {
        Long userId = currentUserService.requireCurrentUserId();
        List<Long> profileIds = userAccessService.findAccessibleProfileIds();
        return mergeRecords(
                safeList(() -> learningPathAdviceRepository.findTop20ByUserAccountIdOrderByCreatedAtDescIdDesc(userId),
                        "Failed to load strength plan records."),
                profileIds.isEmpty()
                        ? List.of()
                        : safeList(() -> learningPathAdviceRepository.findTop20ByUserProfileIdInAndUserAccountIdIsNullOrderByCreatedAtDescIdDesc(profileIds),
                        "Failed to load legacy strength plan records.")
        ).stream().limit(10).toList();
    }

    public List<InterviewSessionSummary> loadInterviewSessions() {
        Long userId = currentUserService.requireCurrentUserId();
        List<Long> profileIds = userAccessService.findAccessibleProfileIds();
        List<InterviewRecord> interviewRecords = mergeRecords(
                safeList(() -> interviewRecordRepository.findTop50ByUserAccountIdOrderByCreatedAtDescIdDesc(userId),
                        "Failed to load interview records."),
                profileIds.isEmpty()
                        ? List.of()
                        : safeList(() -> interviewRecordRepository.findTop50ByUserProfileIdInAndUserAccountIdIsNullOrderByCreatedAtDescIdDesc(profileIds),
                        "Failed to load legacy interview records.")
        );
        return safeList(
                () -> interviewService.buildSessionSummaries(interviewRecords),
                "Failed to build interview session summaries."
        );
    }

    public JourneyContext buildLatestJourney(List<JobMatchRecord> matchRecords,
                                             List<ResumeReview> resumeReviews,
                                             List<ResumeEnhanceRecord> resumeEnhanceRecords,
                                             List<LearningPathAdvice> strengthPlanRecords,
                                             List<InterviewSessionSummary> interviewSessions) {
        Long userProfileId = null;
        Long jobPostId = null;

        if (!matchRecords.isEmpty()) {
            userProfileId = matchRecords.get(0).getUserProfileId();
            jobPostId = matchRecords.get(0).getJobPostId();
        } else if (!strengthPlanRecords.isEmpty()) {
            userProfileId = strengthPlanRecords.get(0).getUserProfileId();
            jobPostId = strengthPlanRecords.get(0).getTargetJobId();
        } else if (!resumeEnhanceRecords.isEmpty()) {
            userProfileId = resumeEnhanceRecords.get(0).getUserProfileId();
            jobPostId = resumeEnhanceRecords.get(0).getJobPostId();
        } else if (!interviewSessions.isEmpty()) {
            userProfileId = interviewSessions.get(0).getUserProfileId();
            jobPostId = interviewSessions.get(0).getJobPostId();
        } else if (!resumeReviews.isEmpty()) {
            userProfileId = resumeReviews.get(0).getUserProfileId();
            jobPostId = resumeReviews.get(0).getJobPostId();
        }

        if (userProfileId == null || jobPostId == null) {
            return null;
        }

        Long selectedUserProfileId = userProfileId;
        Long selectedJobPostId = jobPostId;

        JobMatchRecord latestMatchRecord = jobMatchRecordRepository
                .findTopByUserProfileIdAndJobPostIdOrderByCreatedAtDescIdDesc(selectedUserProfileId, selectedJobPostId)
                .orElse(null);
        ResumeReview latestResumeReview = resumeReviewRepository
                .findTopByUserProfileIdAndJobPostIdOrderByIdDesc(selectedUserProfileId, selectedJobPostId)
                .orElse(null);
        ResumeEnhanceRecord latestEnhanceRecord = resumeEnhanceRecordRepository
                .findTopByUserProfileIdAndJobPostIdOrderByCreatedAtDescIdDesc(selectedUserProfileId, selectedJobPostId)
                .orElse(null);
        LearningPathAdvice latestStrengthPlan = learningPathAdviceRepository
                .findTop10ByUserProfileIdOrderByCreatedAtDescIdDesc(selectedUserProfileId)
                .stream()
                .filter(record -> Objects.equals(record.getTargetJobId(), selectedJobPostId))
                .findFirst()
                .orElse(null);
        InterviewSessionSummary latestInterviewSession = resolveLatestInterviewSession(selectedUserProfileId, selectedJobPostId);

        return new JourneyContext(
                selectedUserProfileId,
                selectedJobPostId,
                latestMatchRecord,
                latestResumeReview,
                latestEnhanceRecord,
                latestStrengthPlan,
                latestInterviewSession
        );
    }

    public Map<Long, String> buildUserNameMap() {
        Map<Long, String> map = new HashMap<>();
        try {
            for (UserProfile profile : userAccessService.findAccessibleProfiles()) {
                map.put(profile.getId(), profile.getStudentName());
            }
        } catch (Exception ex) {
            log.error("Failed to build user name map.", ex);
        }
        return map;
    }

    public Map<Long, String> buildJobNameMap() {
        Map<Long, String> map = new HashMap<>();
        try {
            for (JobPost jobPost : jobPostRepository.findAll()) {
                map.put(jobPost.getId(), jobPost.getJobName());
            }
        } catch (Exception ex) {
            log.error("Failed to build job name map.", ex);
        }
        return map;
    }

    public Map<Long, String> buildStrengthPlanRelatedMatchSummaryMap(List<LearningPathAdvice> records) {
        Map<Long, String> map = new HashMap<>();
        for (LearningPathAdvice record : records) {
            if (record.getUserProfileId() == null || record.getTargetJobId() == null) {
                map.put(record.getId(), "暂无关联岗位匹配");
                continue;
            }

            JobMatchRecord matchRecord = jobMatchRecordRepository
                    .findTopByUserProfileIdAndJobPostIdOrderByCreatedAtDescIdDesc(record.getUserProfileId(), record.getTargetJobId())
                    .orElse(null);
            if (matchRecord == null) {
                map.put(record.getId(), "暂无关联岗位匹配");
                continue;
            }

            String scoreText = matchRecord.getMatchScore() == null ? "-" : String.valueOf(matchRecord.getMatchScore());
            map.put(record.getId(), "匹配记录 #" + matchRecord.getId() + " / 分数 " + scoreText);
        }
        return map;
    }

    public Map<Long, Long> buildStrengthPlanRelatedMatchIdMap(List<LearningPathAdvice> records) {
        Map<Long, Long> map = new HashMap<>();
        for (LearningPathAdvice record : records) {
            if (record.getUserProfileId() == null || record.getTargetJobId() == null) {
                continue;
            }
            jobMatchRecordRepository
                    .findTopByUserProfileIdAndJobPostIdOrderByCreatedAtDescIdDesc(record.getUserProfileId(), record.getTargetJobId())
                    .ifPresent(matchRecord -> map.put(record.getId(), matchRecord.getId()));
        }
        return map;
    }

    public Map<Long, Long> buildStrengthPlanRelatedResumeEnhanceIdMap(List<LearningPathAdvice> records) {
        Map<Long, Long> map = new HashMap<>();
        for (LearningPathAdvice record : records) {
            if (record.getUserProfileId() == null || record.getTargetJobId() == null) {
                continue;
            }
            resumeEnhanceRecordRepository
                    .findTopByUserProfileIdAndJobPostIdOrderByCreatedAtDescIdDesc(record.getUserProfileId(), record.getTargetJobId())
                    .ifPresent(resumeEnhanceRecord -> map.put(record.getId(), resumeEnhanceRecord.getId()));
        }
        return map;
    }

    private InterviewSessionSummary resolveLatestInterviewSession(Long userProfileId, Long jobPostId) {
        List<InterviewRecord> interviewRecords = interviewRecordRepository
                .findTop30ByUserProfileIdOrderByCreatedAtDescIdDesc(userProfileId)
                .stream()
                .filter(record -> Objects.equals(record.getJobPostId(), jobPostId))
                .toList();
        if (interviewRecords.isEmpty()) {
            return null;
        }

        List<InterviewSessionSummary> summaries = interviewService.buildSessionSummaries(interviewRecords);
        return summaries.isEmpty() ? null : summaries.get(0);
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

    private <T> List<T> mergeRecords(List<T> primary, List<T> legacy) {
        return Stream.concat(primary.stream(), legacy.stream())
                .distinct()
                .toList();
    }

    public record JourneyContext(Long userProfileId,
                                 Long jobPostId,
                                 JobMatchRecord matchRecord,
                                 ResumeReview resumeReview,
                                 ResumeEnhanceRecord resumeEnhanceRecord,
                                 LearningPathAdvice learningPathAdvice,
                                 InterviewSessionSummary interviewSession) {
    }
}
