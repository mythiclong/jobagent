package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.dto.JobMatchAiEnhancementResult;
import com.zhihang.jobagent.dto.JobMatchPreferenceInput;
import com.zhihang.jobagent.dto.JobMatchResult;
import com.zhihang.jobagent.entity.JobMatchRecord;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.JobMatchRecordRepository;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.UserProfileRepository;
import com.zhihang.jobagent.service.JobMatchEnhancementService;
import com.zhihang.jobagent.service.JobMatchService;
import com.zhihang.jobagent.service.UserAccessService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Controller
public class JobMatchController {

    private final UserProfileRepository userProfileRepository;
    private final JobPostRepository jobPostRepository;
    private final JobMatchRecordRepository jobMatchRecordRepository;
    private final JobMatchService jobMatchService;
    private final JobMatchEnhancementService jobMatchEnhancementService;
    private final UserAccessService userAccessService;

    public JobMatchController(UserProfileRepository userProfileRepository,
                              JobPostRepository jobPostRepository,
                              JobMatchRecordRepository jobMatchRecordRepository,
                              JobMatchService jobMatchService,
                              JobMatchEnhancementService jobMatchEnhancementService,
                              UserAccessService userAccessService) {
        this.userProfileRepository = userProfileRepository;
        this.jobPostRepository = jobPostRepository;
        this.jobMatchRecordRepository = jobMatchRecordRepository;
        this.jobMatchService = jobMatchService;
        this.jobMatchEnhancementService = jobMatchEnhancementService;
        this.userAccessService = userAccessService;
    }

    @GetMapping("/match")
    public String selectProfile(Model model) {
        model.addAttribute("profiles", userAccessService.findAccessibleProfiles());
        return "match/select";
    }

    @GetMapping("/match/{profileId}")
    public String showMatchResult(@PathVariable Long profileId,
                                  @RequestParam(required = false) String preferredCity,
                                  @RequestParam(required = false) String preferredSalary,
                                  @RequestParam(required = false) String preferredRole,
                                  @RequestParam(required = false) String preferredJobNature,
                                  @RequestParam(required = false) String preferredIndustry,
                                  Model model) {
        UserProfile userProfile = userAccessService.requireAccessibleProfile(profileId);
        JobMatchPreferenceInput preferenceInput = JobMatchPreferenceInput.fromProfile(userProfile);
        preferenceInput.applyTemporaryOverrides(
                preferredCity,
                preferredSalary,
                preferredRole,
                preferredJobNature,
                preferredIndustry
        );

        List<JobPost> jobPosts = jobPostRepository.findAll();
        List<JobMatchResult> matchResults = jobMatchService.matchJobs(userProfile, jobPosts, preferenceInput);
        JobMatchAiEnhancementResult aiEnhancementResult = jobMatchEnhancementService.enhanceAndApply(
                userProfile,
                preferenceInput,
                matchResults
        );
        saveMatchRecords(userProfile, preferenceInput, matchResults);

        model.addAttribute("userProfile", userProfile);
        model.addAttribute("preferenceInput", preferenceInput);
        model.addAttribute("preferenceSummaryText", preferenceInput.toSummaryText());
        model.addAttribute("preferenceSummaryItems", preferenceInput.toSummaryItems());
        model.addAttribute("matchResults", matchResults);
        model.addAttribute("matchResultCount", matchResults.size());
        model.addAttribute("highestCompositeScore", highestCompositeScore(matchResults));
        model.addAttribute("averagePreferenceFitPercent", averagePreferenceFitPercent(matchResults));
        model.addAttribute("highRiskJobCount", highRiskJobCount(matchResults));
        model.addAttribute("featuredResult", matchResults.isEmpty() ? null : matchResults.get(0));
        model.addAttribute("matchAiSummary", aiEnhancementResult.getSummary());
        model.addAttribute("matchAiMetadata", aiEnhancementResult.getMetadata());
        model.addAttribute("matchAiSourceLabel", sourceLabel(aiEnhancementResult.getMetadata()));
        return "match/result";
    }

    @GetMapping("/match/record/{id}")
    public String showMatchRecordDetail(@PathVariable Long id, Model model) {
        JobMatchRecord record = jobMatchRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid match record id: " + id));
        userAccessService.assertCanAccessRecord(record.getUserAccountId(), record.getUserProfileId());

        UserProfile userProfile = userProfileRepository.findById(record.getUserProfileId()).orElse(null);
        JobPost jobPost = jobPostRepository.findById(record.getJobPostId()).orElse(null);

        model.addAttribute("record", record);
        model.addAttribute("userProfile", userProfile);
        model.addAttribute("jobPost", jobPost);
        return "match/detail";
    }

    private void saveMatchRecords(UserProfile userProfile,
                                  JobMatchPreferenceInput preferenceInput,
                                  List<JobMatchResult> matchResults) {
        if (matchResults.isEmpty()) {
            return;
        }

        List<JobMatchRecord> records = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (JobMatchResult result : matchResults) {
            if (result.getJobPost() == null || result.getJobPost().getId() == null) {
                continue;
            }
            JobMatchRecord record = new JobMatchRecord();
            record.setUserAccountId(userProfile.getUserAccountId());
            record.setUserProfileId(userProfile.getId());
            record.setJobPostId(result.getJobPost().getId());
            record.setMatchScore(result.getMatchScore());
            record.setCompatibilityLevel(result.getCompatibilityLevel());
            record.setMatchReason(result.getMatchReason());
            record.setHardConditionSummary(result.getHardConditionSummary());
            record.setScoreBreakdown(result.getScoreBreakdown());
            record.setWeaknessAnalysis(result.getWeaknessAnalysis());
            record.setDeductionReasons(result.getDeductionReasons());
            record.setImprovementSuggestion(result.getImprovementSuggestion());
            record.setAiRerankScore(result.getAiRerankScore());
            record.setCompositeRankScore(result.getCompositeRankScore());
            record.setPreferenceFitLevel(result.getPreferenceFitLevel());
            record.setAiMatchReason(result.getAiMatchReason());
            record.setAiRiskPoints(result.getAiRiskPoints());
            record.setRecommendedPriority(result.getRecommendedPriority());
            record.setSuggestedNextAction(result.getSuggestedNextAction());
            record.setShortTagSummary(result.getShortTagSummary());
            record.setPreferenceSummary(result.getPreferenceSummary() != null
                    ? result.getPreferenceSummary()
                    : (preferenceInput == null ? "" : preferenceInput.toSummaryText()));
            record.setCreatedAt(now);
            records.add(record);
        }
        if (!records.isEmpty()) {
            jobMatchRecordRepository.saveAll(records);
        }
    }

    private String sourceLabel(com.zhihang.jobagent.dto.AiGenerationMetadata metadata) {
        if (metadata == null) {
            return "Rule";
        }
        return switch (metadata.getGenerationSource()) {
            case GEMINI -> "Gemini";
            case QWEN -> "Qwen";
            case FALLBACK -> "Rule";
        };
    }

    private int highestCompositeScore(List<JobMatchResult> matchResults) {
        return matchResults.stream()
                .mapToInt(JobMatchResult::getDisplayCompositeScore)
                .max()
                .orElse(0);
    }

    private int averagePreferenceFitPercent(List<JobMatchResult> matchResults) {
        if (matchResults == null || matchResults.isEmpty()) {
            return 0;
        }
        return (int) Math.round(matchResults.stream()
                .mapToInt(JobMatchResult::getPreferenceFitPercent)
                .average()
                .orElse(0));
    }

    private long highRiskJobCount(List<JobMatchResult> matchResults) {
        return matchResults.stream()
                .filter(result -> "high".equalsIgnoreCase(result.getRiskLevelKey()))
                .count();
    }
}
