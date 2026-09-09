package com.zhihang.jobagent.service;

import com.zhihang.jobagent.dto.AiGenerationMetadata;
import com.zhihang.jobagent.dto.JobMatchAiEnhancementResult;
import com.zhihang.jobagent.dto.JobMatchAiInsight;
import com.zhihang.jobagent.dto.JobMatchPreferenceInput;
import com.zhihang.jobagent.dto.JobMatchResult;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.ResumeEnhanceRecordRepository;
import com.zhihang.jobagent.repository.ResumeReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class JobMatchEnhancementService {

    private static final int MAX_AI_CANDIDATES = 24;

    private final AiFacadeService aiFacadeService;
    private final ResumeEnhanceRecordRepository resumeEnhanceRecordRepository;
    private final ResumeReviewRepository resumeReviewRepository;

    public JobMatchEnhancementService(AiFacadeService aiFacadeService,
                                      ResumeEnhanceRecordRepository resumeEnhanceRecordRepository,
                                      ResumeReviewRepository resumeReviewRepository) {
        this.aiFacadeService = aiFacadeService;
        this.resumeEnhanceRecordRepository = resumeEnhanceRecordRepository;
        this.resumeReviewRepository = resumeReviewRepository;
    }

    public JobMatchAiEnhancementResult enhanceAndApply(UserProfile userProfile,
                                                       JobMatchPreferenceInput preferenceInput,
                                                       List<JobMatchResult> results) {
        if (results == null || results.isEmpty()) {
            return new JobMatchAiEnhancementResult(
                    Map.of(),
                    "",
                    AiGenerationMetadata.fallback("local-rule", "No match results available for AI enhancement.")
            );
        }

        ResumeEnhanceRecord latestEnhance = loadLatestEnhance(userProfile);
        ResumeReview latestReview = loadLatestReview(userProfile);
        JobMatchAiEnhancementResult aiResult = aiFacadeService.enhanceMatchCandidates(
                userProfile,
                preferenceInput,
                latestEnhance,
                latestReview,
                selectCandidatePool(results)
        );

        applyInsights(results, preferenceInput, aiResult.getInsightByJobPostId());
        sortResults(results);
        return aiResult;
    }

    private List<JobMatchResult> selectCandidatePool(List<JobMatchResult> results) {
        return results.stream()
                .filter(result -> result.getJobPost() != null && result.getJobPost().getId() != null)
                .limit(MAX_AI_CANDIDATES)
                .toList();
    }

    private void applyInsights(List<JobMatchResult> results,
                               JobMatchPreferenceInput preferenceInput,
                               Map<Long, JobMatchAiInsight> insightByJobPostId) {
        for (JobMatchResult result : results) {
            if (result == null || result.getJobPost() == null || result.getJobPost().getId() == null) {
                continue;
            }
            result.setPreferenceSummary(preferenceInput == null ? "" : preferenceInput.toSummaryText());

            JobMatchAiInsight insight = insightByJobPostId == null ? null : insightByJobPostId.get(result.getJobPost().getId());
            if (insight == null) {
                if (result.getCompositeRankScore() == null) {
                    result.setCompositeRankScore(result.getMatchScore());
                }
                continue;
            }

            result.setAiRerankScore(insight.getRerankScore());
            result.setPreferenceFitLevel(insight.getPreferenceFitLevel());
            result.setAiMatchReason(insight.getAiMatchReason());
            result.setAiRiskPoints(joinLines(insight.getAiRiskPoints()));
            result.setRecommendedPriority(insight.getRecommendedPriority());
            result.setSuggestedNextAction(insight.getSuggestedNextAction());
            result.setShortTagSummary(joinLines(insight.getShortTagSummary()));
            result.setCompositeRankScore(computeCompositeRankScore(result));
        }
    }

    private int computeCompositeRankScore(JobMatchResult result) {
        int baseScore = result.getMatchScore() == null ? 0 : result.getMatchScore();
        int aiScore = result.getAiRerankScore() == null ? baseScore : result.getAiRerankScore();
        int priorityBonus = switch (safeText(result.getRecommendedPriority()).toUpperCase(Locale.ROOT)) {
            case "HIGH" -> 4;
            case "MEDIUM" -> 1;
            case "LOW" -> -2;
            default -> 0;
        };
        int preferenceBonus = switch (safeText(result.getPreferenceFitLevel()).toUpperCase(Locale.ROOT)) {
            case "HIGH" -> 3;
            case "MEDIUM" -> 1;
            case "LOW" -> -1;
            default -> 0;
        };

        int composite = Math.round(baseScore * 0.65f + aiScore * 0.35f) + priorityBonus + preferenceBonus;
        if ("MISMATCH".equalsIgnoreCase(safeText(result.getCompatibilityLevel()))) {
            composite = Math.min(composite, 58);
        } else if ("WEAK".equalsIgnoreCase(safeText(result.getCompatibilityLevel()))) {
            composite = Math.min(composite, 82);
        }
        return Math.max(0, Math.min(100, composite));
    }

    private void sortResults(List<JobMatchResult> results) {
        results.sort(Comparator
                .comparingInt((JobMatchResult result) -> defaultInt(result.getCompositeRankScore()))
                .reversed()
                .thenComparing(JobMatchResult::getAiRerankScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(JobMatchResult::getMatchScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(result -> safeText(result.getJobPost() == null ? "" : result.getJobPost().getDisplayTitle())));
    }

    private ResumeEnhanceRecord loadLatestEnhance(UserProfile userProfile) {
        if (userProfile == null || userProfile.getId() == null) {
            return null;
        }
        return resumeEnhanceRecordRepository.findTop10ByUserProfileIdOrderByCreatedAtDescIdDesc(userProfile.getId())
                .stream()
                .findFirst()
                .orElse(null);
    }

    private ResumeReview loadLatestReview(UserProfile userProfile) {
        if (userProfile == null || userProfile.getId() == null) {
            return null;
        }
        return resumeReviewRepository.findTop10ByUserProfileIdOrderByIdDesc(userProfile.getId())
                .stream()
                .findFirst()
                .orElse(null);
    }

    private String joinLines(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }
        return items.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(6)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private int defaultInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String safeText(String text) {
        return text == null ? "" : text.trim();
    }
}
