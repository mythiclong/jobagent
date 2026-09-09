package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.dto.ResumeEnhanceAiResult;
import com.zhihang.jobagent.dto.ResumeParsedData;
import com.zhihang.jobagent.dto.ResumeRewriteBlock;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.JobPostRepository;
import com.zhihang.jobagent.repository.ResumeEnhanceRecordRepository;
import com.zhihang.jobagent.repository.UserProfileRepository;
import com.zhihang.jobagent.service.CurrentUserService;
import com.zhihang.jobagent.service.ResumeEnhanceService;
import com.zhihang.jobagent.service.ResumeProfileSyncService;
import com.zhihang.jobagent.service.UserAccessService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Controller
public class ResumeEnhanceController {

    private final UserProfileRepository userProfileRepository;
    private final JobPostRepository jobPostRepository;
    private final ResumeEnhanceRecordRepository resumeEnhanceRecordRepository;
    private final ResumeEnhanceService resumeEnhanceService;
    private final ResumeProfileSyncService resumeProfileSyncService;
    private final UserAccessService userAccessService;
    private final CurrentUserService currentUserService;

    public ResumeEnhanceController(UserProfileRepository userProfileRepository,
                                   JobPostRepository jobPostRepository,
                                   ResumeEnhanceRecordRepository resumeEnhanceRecordRepository,
                                   ResumeEnhanceService resumeEnhanceService,
                                   ResumeProfileSyncService resumeProfileSyncService,
                                   UserAccessService userAccessService,
                                   CurrentUserService currentUserService) {
        this.userProfileRepository = userProfileRepository;
        this.jobPostRepository = jobPostRepository;
        this.resumeEnhanceRecordRepository = resumeEnhanceRecordRepository;
        this.resumeEnhanceService = resumeEnhanceService;
        this.resumeProfileSyncService = resumeProfileSyncService;
        this.userAccessService = userAccessService;
        this.currentUserService = currentUserService;
    }

    @GetMapping({"/resume-enhance", "/resume-optimize"})
    public String showForm(@RequestParam(required = false) Long profileId,
                           @RequestParam(required = false) Long jobId,
                           @RequestParam(required = false) String source,
                           Model model) {
        model.addAttribute("profiles", userAccessService.findAccessibleProfiles());
        model.addAttribute("jobs", jobPostRepository.findAll());
        model.addAttribute("selectedProfileId", profileId);
        model.addAttribute("selectedJobId", jobId);
        model.addAttribute("source", source);
        model.addAttribute("geminiEnabled", resumeEnhanceService.isGeminiEnabled());
        return "resume-enhance/form";
    }

    @PostMapping({"/resume-enhance/parse", "/resume-optimize/parse"})
    public String parseResume(@RequestParam(required = false) Long userProfileId,
                              @RequestParam Long jobPostId,
                              @RequestParam(required = false) MultipartFile resumePdf,
                              @RequestParam(required = false) String resumeText,
                              Model model) {
        UserProfile userProfile = userProfileId == null ? null : userAccessService.requireAccessibleProfile(userProfileId);
        JobPost jobPost = findJob(jobPostId);
        ResumeParsedData parsedData = resumeEnhanceService.parseInput(resumePdf, resumeText);

        model.addAttribute("profiles", userAccessService.findAccessibleProfiles());
        model.addAttribute("jobs", jobPostRepository.findAll());
        model.addAttribute("userProfile", userProfile);
        model.addAttribute("jobPost", jobPost);
        model.addAttribute("selectedProfileId", userProfileId);
        model.addAttribute("selectedJobId", jobPostId);
        model.addAttribute("parsedData", parsedData);
        model.addAttribute("geminiEnabled", resumeEnhanceService.isGeminiEnabled());
        model.addAttribute("canAnalyze", userProfile != null);
        model.addAttribute("profileSyncSuggestion", resumeProfileSyncService.buildSuggestion(userProfile, parsedData, jobPost));
        return "resume-enhance/preview";
    }

    @PostMapping({"/resume-enhance/analyze", "/resume-optimize/analyze"})
    public String analyzeResume(@RequestParam Long userProfileId,
                                @RequestParam Long jobPostId,
                                @RequestParam String inputType,
                                @RequestParam String originalResumeText,
                                @RequestParam String parsedBasicInfo,
                                @RequestParam String parsedEducation,
                                @RequestParam String parsedSkills,
                                @RequestParam String parsedProjects,
                                @RequestParam String parsedExperience,
                                @RequestParam String parsedOthers,
                                Model model) {
        UserProfile userProfile = userAccessService.requireAccessibleProfile(userProfileId);
        JobPost jobPost = findJob(jobPostId);
        ResumeParsedData parsedData = new ResumeParsedData(
                originalResumeText,
                inputType,
                parsedBasicInfo,
                parsedEducation,
                parsedSkills,
                parsedProjects,
                parsedExperience,
                parsedOthers
        );

        ResumeEnhanceRecord record = resumeEnhanceService.analyzeAndSave(userProfile, jobPost, parsedData);
        fillResultModel(model, record, userProfile, jobPost);
        return "resume-enhance/result";
    }

    @PostMapping({"/resume-enhance/rewrite-section", "/resume-optimize/rewrite-section"})
    public String rewriteSection(@RequestParam Long recordId,
                                 @RequestParam String sectionKey,
                                 Model model) {
        ResumeEnhanceRecord record = findAccessibleRecord(recordId);
        UserProfile userProfile = findUserProfile(record.getUserProfileId());
        JobPost jobPost = findJob(record.getJobPostId());

        ResumeEnhanceRecord updatedRecord = resumeEnhanceService.rewriteSection(record, sectionKey, userProfile, jobPost);
        fillResultModel(model, updatedRecord, userProfile, jobPost);
        model.addAttribute("notice", displaySectionName(sectionKey) + "已更新。");
        return "resume-enhance/result";
    }

    @PostMapping({"/resume-enhance/rewrite-section-json", "/resume-optimize/rewrite-section-json"})
    @ResponseBody
    public Map<String, Object> rewriteSectionJson(@RequestParam Long recordId,
                                                  @RequestParam String sectionKey) {
        ResumeEnhanceRecord record = findAccessibleRecord(recordId);
        UserProfile userProfile = findUserProfile(record.getUserProfileId());
        JobPost jobPost = findJob(record.getJobPostId());
        ResumeEnhanceRecord updatedRecord = resumeEnhanceService.rewriteSection(record, sectionKey, userProfile, jobPost);
        ResumeEnhanceAiResult aiResult = resumeEnhanceService.buildAiResult(updatedRecord, userProfile, jobPost);

        ResumeRewriteBlock block = resolveRewriteBlock(aiResult, sectionKey);
        String normalizedSectionKey = normalizeSectionKey(sectionKey);
        Map<String, String> sectionSourceMap = resumeEnhanceService.buildSectionGenerationSourceMap(updatedRecord);
        Map<String, String> sectionFallbackReasonMap = resumeEnhanceService.buildSectionFallbackReasonMap(updatedRecord);
        Map<String, String> sectionModelMap = resumeEnhanceService.buildSectionModelMap(updatedRecord);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("recordId", updatedRecord.getId());
        payload.put("sectionKey", normalizedSectionKey);
        payload.put("sectionTitle", displaySectionName(normalizedSectionKey));
        payload.put("original", block == null ? "" : block.getOriginal());
        payload.put("rewritten", block == null ? "" : block.getRewritten());
        payload.put("reason", block == null ? "" : block.getReason());
        payload.put("source", sectionSourceMap.getOrDefault(normalizedSectionKey, ""));
        payload.put("sourceLabel", sourceLabel(sectionSourceMap.getOrDefault(normalizedSectionKey, "")));
        payload.put("modelName", sectionModelMap.getOrDefault(normalizedSectionKey, ""));
        payload.put("fallbackReason", sectionFallbackReasonMap.getOrDefault(normalizedSectionKey, ""));
        payload.put("notice", displaySectionName(normalizedSectionKey) + "已更新。");
        return payload;
    }

    @PostMapping({"/resume-enhance/rewrite-full", "/resume-optimize/rewrite-full"})
    public String rewriteFull(@RequestParam Long recordId, Model model) {
        ResumeEnhanceRecord record = findAccessibleRecord(recordId);
        UserProfile userProfile = findUserProfile(record.getUserProfileId());
        JobPost jobPost = findJob(record.getJobPostId());

        ResumeEnhanceRecord updatedRecord = resumeEnhanceService.rewriteFullResume(record, userProfile, jobPost);
        fillResultModel(model, updatedRecord, userProfile, jobPost);
        model.addAttribute("notice", "已生成整份优化版简历。");
        return "resume-enhance/result";
    }

    @PostMapping({"/resume-enhance/rewrite-full-json", "/resume-optimize/rewrite-full-json"})
    @ResponseBody
    public Map<String, Object> rewriteFullJson(@RequestParam Long recordId) {
        ResumeEnhanceRecord record = findAccessibleRecord(recordId);
        UserProfile userProfile = findUserProfile(record.getUserProfileId());
        JobPost jobPost = findJob(record.getJobPostId());
        ResumeEnhanceRecord updatedRecord = resumeEnhanceService.rewriteFullResume(record, userProfile, jobPost);
        ResumeEnhanceAiResult aiResult = resumeEnhanceService.buildAiResult(updatedRecord, userProfile, jobPost);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", true);
        payload.put("recordId", updatedRecord.getId());
        payload.put("fullOptimizedResume", aiResult.getFullOptimizedResume());
        payload.put("source", updatedRecord.getFullResumeGenerationSource());
        payload.put("sourceLabel", sourceLabel(updatedRecord.getFullResumeGenerationSource()));
        payload.put("modelName", updatedRecord.getFullResumeModelName());
        payload.put("fallbackReason", updatedRecord.getFullResumeFallbackReason());
        payload.put("notice", "已生成整份优化版简历。");
        return payload;
    }

    @GetMapping({"/resume-enhance/history", "/resume-optimize/history"})
    public String showHistory(Model model) {
        model.addAttribute("records", loadCurrentUserRecords());
        model.addAttribute("userNameMap", buildUserNameMap());
        model.addAttribute("jobNameMap", buildJobNameMap());
        return "resume-enhance/history";
    }

    @GetMapping({"/resume-enhance/{id}", "/resume-optimize/{id}"})
    public String showDetail(@PathVariable Long id, Model model) {
        ResumeEnhanceRecord record = findAccessibleRecord(id);
        UserProfile userProfile = findUserProfile(record.getUserProfileId());
        JobPost jobPost = findJob(record.getJobPostId());
        fillResultModel(model, record, userProfile, jobPost);
        return "resume-enhance/result";
    }

    private void fillResultModel(Model model, ResumeEnhanceRecord record, UserProfile userProfile, JobPost jobPost) {
        model.addAttribute("aiResult", resumeEnhanceService.buildAiResult(record, userProfile, jobPost));
        model.addAttribute("record", record);
        model.addAttribute("userProfile", userProfile);
        model.addAttribute("jobPost", jobPost);
        model.addAttribute("originalSections", buildOriginalSections(record));
        model.addAttribute("optimizedSections", resumeEnhanceService.buildSectionMap(record, true));
        model.addAttribute("sectionSourceMap", resumeEnhanceService.buildSectionGenerationSourceMap(record));
        model.addAttribute("sectionFallbackReasonMap", resumeEnhanceService.buildSectionFallbackReasonMap(record));
        model.addAttribute("sectionModelMap", resumeEnhanceService.buildSectionModelMap(record));
        model.addAttribute("geminiEnabled", resumeEnhanceService.isGeminiEnabled());
    }

    private ResumeRewriteBlock resolveRewriteBlock(ResumeEnhanceAiResult aiResult, String sectionKey) {
        return switch (normalizeSectionKey(sectionKey)) {
            case "education" -> aiResult.getEducationRewrite();
            case "skills" -> aiResult.getSkillsRewrite();
            case "projects" -> aiResult.getProjectsRewrite();
            case "experience" -> aiResult.getExperienceRewrite();
            default -> null;
        };
    }

    private String normalizeSectionKey(String sectionKey) {
        if (sectionKey == null) {
            return "";
        }
        return switch (sectionKey.trim()) {
            case "edu" -> "education";
            case "project" -> "projects";
            case "exp" -> "experience";
            default -> sectionKey.trim();
        };
    }

    private String sourceLabel(String source) {
        if ("GEMINI".equalsIgnoreCase(source)) {
            return "Gemini";
        }
        if ("QWEN".equalsIgnoreCase(source)) {
            return "Qwen";
        }
        if ("FALLBACK".equalsIgnoreCase(source)) {
            return "Fallback";
        }
        return "Pending";
    }

    private Map<String, String> buildOriginalSections(ResumeEnhanceRecord record) {
        Map<String, String> sections = new HashMap<>();
        sections.put("basicInfo", record.getParsedBasicInfo());
        sections.put("education", record.getParsedEducation());
        sections.put("skills", record.getParsedSkills());
        sections.put("projects", record.getParsedProjects());
        sections.put("experience", record.getParsedExperience());
        sections.put("others", record.getParsedOthers());
        return sections;
    }

    private Map<Long, String> buildUserNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (UserProfile profile : userAccessService.findAccessibleProfiles()) {
            map.put(profile.getId(), profile.getStudentName());
        }
        return map;
    }

    private Map<Long, String> buildJobNameMap() {
        Map<Long, String> map = new HashMap<>();
        for (JobPost jobPost : jobPostRepository.findAll()) {
            map.put(jobPost.getId(), jobPost.getJobName());
        }
        return map;
    }

    private ResumeEnhanceRecord findAccessibleRecord(Long id) {
        ResumeEnhanceRecord record = resumeEnhanceRecordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid resume enhance record id: " + id));
        userAccessService.assertCanAccessRecord(record.getUserAccountId(), record.getUserProfileId());
        return record;
    }

    private UserProfile findUserProfile(Long id) {
        return id == null ? null : userProfileRepository.findById(id).orElse(null);
    }

    private JobPost findJob(Long id) {
        return jobPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid job id: " + id));
    }

    private String displaySectionName(String sectionKey) {
        if (sectionKey == null) {
            return "当前模块";
        }
        return switch (normalizeSectionKey(sectionKey)) {
            case "basicInfo" -> "基本信息";
            case "education" -> "教育经历";
            case "skills" -> "技能描述";
            case "projects" -> "项目经历";
            case "experience" -> "实习与校园经历";
            case "others" -> "其他信息";
            default -> "当前模块";
        };
    }

    private List<ResumeEnhanceRecord> loadCurrentUserRecords() {
        Long userId = currentUserService.requireCurrentUserId();
        List<Long> profileIds = userAccessService.findAccessibleProfileIds();
        return Stream.concat(
                        resumeEnhanceRecordRepository.findTop20ByUserAccountIdOrderByCreatedAtDescIdDesc(userId).stream(),
                        profileIds.isEmpty()
                                ? Stream.empty()
                                : resumeEnhanceRecordRepository.findTop20ByUserProfileIdInAndUserAccountIdIsNullOrderByCreatedAtDescIdDesc(profileIds).stream()
                )
                .distinct()
                .limit(20)
                .toList();
    }
}
