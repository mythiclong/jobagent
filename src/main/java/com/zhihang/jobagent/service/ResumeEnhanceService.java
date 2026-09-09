package com.zhihang.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihang.jobagent.dto.AiGenerationMetadata;
import com.zhihang.jobagent.dto.AiTextGenerationResult;
import com.zhihang.jobagent.dto.ResumeAiGenerationResult;
import com.zhihang.jobagent.dto.ResumeAiReviewResult;
import com.zhihang.jobagent.dto.ResumeEnhanceAiResult;
import com.zhihang.jobagent.dto.ResumeParsedData;
import com.zhihang.jobagent.dto.ResumeRewriteBlock;
import com.zhihang.jobagent.dto.ResumeSectionRewriteGenerationResult;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.ResumeEnhanceRecord;
import com.zhihang.jobagent.entity.ResumeReview;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.ResumeEnhanceRecordRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ResumeEnhanceService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_PROBLEMS = 4;
    private static final int MAX_KEYWORDS = 12;
    private static final int MAX_SUGGESTIONS = 5;

    private final ResumeEnhanceRecordRepository resumeEnhanceRecordRepository;
    private final ResumeReviewService resumeReviewService;
    private final AiFacadeService aiFacadeService;
    private final AiTextSanitizer aiTextSanitizer;

    public ResumeEnhanceService(ResumeEnhanceRecordRepository resumeEnhanceRecordRepository,
                                ResumeReviewService resumeReviewService,
                                AiFacadeService aiFacadeService,
                                AiTextSanitizer aiTextSanitizer) {
        this.resumeEnhanceRecordRepository = resumeEnhanceRecordRepository;
        this.resumeReviewService = resumeReviewService;
        this.aiFacadeService = aiFacadeService;
        this.aiTextSanitizer = aiTextSanitizer;
    }

    public boolean isGeminiEnabled() {
        return aiFacadeService.isGeminiEnabled();
    }

    public ResumeParsedData parseInput(MultipartFile pdfFile, String resumeText) {
        String normalizedText = normalizeText(resumeText);
        String inputType = "TEXT";
        if (!StringUtils.hasText(normalizedText) && pdfFile != null && !pdfFile.isEmpty()) {
            normalizedText = parsePdf(pdfFile);
            inputType = "PDF";
        }
        if (!StringUtils.hasText(normalizedText)) {
            throw new IllegalArgumentException("Please upload PDF or paste resume text.");
        }
        return splitResume(normalizedText, inputType);
    }

    public ResumeEnhanceRecord analyzeAndSave(UserProfile userProfile, JobPost jobPost, ResumeParsedData parsedData) {
        ResumeReview baseReview = resumeReviewService.analyze(userProfile, jobPost, parsedData.getOriginalText());
        ResumeAiGenerationResult aiReviewResult = aiFacadeService.generateReview(parsedData.getOriginalText(), jobPost, userProfile);
        ResumeAiReviewResult aiReview = aiReviewResult.getReview();
        AiGenerationMetadata reviewMetadata = aiReviewResult.getMetadata();

        List<String> problems = sanitizeListItems(mergeUnique(
                splitToList(baseReview.getProblems(), MAX_PROBLEMS),
                parseJsonArrayOrSplit(aiReview.getProblems(), MAX_PROBLEMS),
                MAX_PROBLEMS
        ), MAX_PROBLEMS);
        List<String> missingKeywords = sanitizeListItems(mergeUnique(
                splitToList(baseReview.getMissingKeywords(), MAX_KEYWORDS),
                parseJsonArrayOrSplit(aiReview.getMissingKeywords(), MAX_KEYWORDS),
                MAX_KEYWORDS
        ), MAX_KEYWORDS);
        List<String> suggestions = sanitizeListItems(mergeUnique(
                splitToList(baseReview.getSuggestions(), MAX_SUGGESTIONS),
                parseJsonArrayOrSplit(aiReview.getSuggestions(), MAX_SUGGESTIONS),
                MAX_SUGGESTIONS
        ), MAX_SUGGESTIONS);

        ResumeEnhanceRecord record = new ResumeEnhanceRecord();
        record.setUserAccountId(userProfile.getUserAccountId());
        record.setUserProfileId(userProfile.getId());
        record.setJobPostId(jobPost.getId());
        record.setInputType(parsedData.getInputType());
        record.setOriginalResumeText(aiTextSanitizer.sanitizeResumeText(parsedData.getOriginalText()));
        record.setParsedBasicInfo(aiTextSanitizer.sanitizeResumeText(parsedData.getBasicInfo()));
        record.setParsedEducation(aiTextSanitizer.sanitizeResumeText(parsedData.getEducation()));
        record.setParsedSkills(aiTextSanitizer.sanitizeResumeText(parsedData.getSkills()));
        record.setParsedProjects(aiTextSanitizer.sanitizeResumeText(parsedData.getProjects()));
        record.setParsedExperience(aiTextSanitizer.sanitizeResumeText(parsedData.getExperience()));
        record.setParsedOthers(aiTextSanitizer.sanitizeResumeText(parsedData.getOthers()));
        record.setReviewScore(baseReview.getReviewScore());
        record.setAiSummary(limitSentence(
                aiTextSanitizer.sanitizePlainText(defaultIfEmpty(aiReview.getSummary(), baseReview.getSummary())),
                140
        ));
        record.setAiProblems(writeJsonArray(problems));
        record.setAiMissingKeywords(writeJsonArray(missingKeywords));
        record.setAiSuggestions(writeJsonArray(suggestions));
        record.setReviewGenerationSource(reviewMetadata.getGenerationSource().name());
        record.setReviewAiUsed(reviewMetadata.isAiUsed());
        record.setReviewFallbackReason(composeFallbackReason(reviewMetadata));
        record.setReviewModelName(reviewMetadata.getModelName());
        record.setCreatedAt(LocalDateTime.now());
        return resumeEnhanceRecordRepository.save(record);
    }

    public ResumeEnhanceRecord rewriteSection(ResumeEnhanceRecord record, String sectionKey,
                                              UserProfile userProfile, JobPost jobPost) {
        String normalizedKey = normalizeSectionKey(sectionKey);
        String sourceText = getSectionText(record, normalizedKey);
        if (!StringUtils.hasText(sourceText)) return record;
        String sectionName = displaySectionName(normalizedKey);
        ResumeSectionRewriteGenerationResult optimizedResult =
                aiFacadeService.rewriteSectionStructured(sectionName, sourceText, jobPost, userProfile);
        String rewritten = defaultIfEmpty(
                aiTextSanitizer.sanitizeResumeText(optimizedResult.getRewritten()),
                aiTextSanitizer.sanitizeResumeText(sourceText)
        );
        String reason = defaultIfEmpty(aiTextSanitizer.sanitizePlainText(optimizedResult.getReason()), "Section rewritten.");
        setOptimizedSection(record, normalizedKey, serializeRewriteBlock(sourceText, rewritten, reason));
        setSectionMetadata(record, normalizedKey, optimizedResult.getMetadata());
        return resumeEnhanceRecordRepository.save(record);
    }

    public ResumeEnhanceRecord rewriteFullResume(ResumeEnhanceRecord record, UserProfile userProfile, JobPost jobPost) {
        Map<String, String> sections = buildSectionMap(record, true);
        AiTextGenerationResult optimizedResult =
                aiFacadeService.rewriteFull(record.getOriginalResumeText(), sections, jobPost, userProfile);
        record.setFullOptimizedResume(aiTextSanitizer.sanitizeResumeText(optimizedResult.getText()));
        setFullResumeMetadata(record, optimizedResult.getMetadata());
        return resumeEnhanceRecordRepository.save(record);
    }

    public Map<String, String> buildSectionMap(ResumeEnhanceRecord record, boolean optimizedFirst) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("basicInfo", sectionValue(optimizedFirst ? record.getOptimizedBasicInfo() : null, record.getParsedBasicInfo()));
        map.put("education", sectionValue(optimizedFirst ? record.getOptimizedEducation() : null, record.getParsedEducation()));
        map.put("skills", sectionValue(optimizedFirst ? record.getOptimizedSkills() : null, record.getParsedSkills()));
        map.put("projects", sectionValue(optimizedFirst ? record.getOptimizedProjects() : null, record.getParsedProjects()));
        map.put("experience", sectionValue(optimizedFirst ? record.getOptimizedExperience() : null, record.getParsedExperience()));
        map.put("others", sectionValue(optimizedFirst ? record.getOptimizedOthers() : null, record.getParsedOthers()));
        return map;
    }

    public Map<String, String> buildSectionGenerationSourceMap(ResumeEnhanceRecord record) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("basicInfo", normalizeText(record.getBasicInfoGenerationSource()));
        map.put("education", normalizeText(record.getEducationGenerationSource()));
        map.put("skills", normalizeText(record.getSkillsGenerationSource()));
        map.put("projects", normalizeText(record.getProjectsGenerationSource()));
        map.put("experience", normalizeText(record.getExperienceGenerationSource()));
        map.put("others", normalizeText(record.getOthersGenerationSource()));
        return map;
    }

    public Map<String, String> buildSectionFallbackReasonMap(ResumeEnhanceRecord record) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("basicInfo", aiTextSanitizer.sanitizePlainText(record.getBasicInfoFallbackReason()));
        map.put("education", aiTextSanitizer.sanitizePlainText(record.getEducationFallbackReason()));
        map.put("skills", aiTextSanitizer.sanitizePlainText(record.getSkillsFallbackReason()));
        map.put("projects", aiTextSanitizer.sanitizePlainText(record.getProjectsFallbackReason()));
        map.put("experience", aiTextSanitizer.sanitizePlainText(record.getExperienceFallbackReason()));
        map.put("others", aiTextSanitizer.sanitizePlainText(record.getOthersFallbackReason()));
        return map;
    }

    public Map<String, String> buildSectionModelMap(ResumeEnhanceRecord record) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("basicInfo", normalizeText(record.getBasicInfoModelName()));
        map.put("education", normalizeText(record.getEducationModelName()));
        map.put("skills", normalizeText(record.getSkillsModelName()));
        map.put("projects", normalizeText(record.getProjectsModelName()));
        map.put("experience", normalizeText(record.getExperienceModelName()));
        map.put("others", normalizeText(record.getOthersModelName()));
        return map;
    }

    public ResumeEnhanceAiResult buildAiResult(ResumeEnhanceRecord record, UserProfile userProfile, JobPost jobPost) {
        List<String> coreProblems = sanitizeListItems(parseJsonArrayOrSplit(record.getAiProblems(), MAX_PROBLEMS), MAX_PROBLEMS);
        if (coreProblems.isEmpty()) coreProblems = List.of("Insufficient issue details.");

        List<String> missingKeywords = sanitizeListItems(parseJsonArrayOrSplit(record.getAiMissingKeywords(), MAX_KEYWORDS), MAX_KEYWORDS);
        if (missingKeywords.isEmpty()) missingKeywords = sanitizeListItems(splitToList(jobPost.getJobRequirements(), 6), 6);
        if (missingKeywords.isEmpty()) missingKeywords = List.of("job keyword", "core skill", "project outcome");

        List<String> improvementSuggestions = ensureSuggestionCount(
                sanitizeListItems(parseJsonArrayOrSplit(record.getAiSuggestions(), MAX_SUGGESTIONS), MAX_SUGGESTIONS),
                userProfile, jobPost
        );

        ResumeRewriteBlock education = toRewriteBlock(record.getParsedEducation(), record.getOptimizedEducation(),
                "education", record.getEducationGenerationSource(), record.getEducationFallbackReason(), jobPost);
        ResumeRewriteBlock skills = toRewriteBlock(record.getParsedSkills(), record.getOptimizedSkills(),
                "skills", record.getSkillsGenerationSource(), record.getSkillsFallbackReason(), jobPost);
        ResumeRewriteBlock projects = toRewriteBlock(record.getParsedProjects(), record.getOptimizedProjects(),
                "projects", record.getProjectsGenerationSource(), record.getProjectsFallbackReason(), jobPost);
        ResumeRewriteBlock experience = toRewriteBlock(record.getParsedExperience(), record.getOptimizedExperience(),
                "experience", record.getExperienceGenerationSource(), record.getExperienceFallbackReason(), jobPost);

        String reviewSource = normalizeText(record.getReviewGenerationSource());
        boolean aiUsed = Boolean.TRUE.equals(record.getReviewAiUsed())
                || "GEMINI".equalsIgnoreCase(reviewSource)
                || "QWEN".equalsIgnoreCase(reviewSource);
        if (!StringUtils.hasText(reviewSource)) reviewSource = aiUsed ? "GEMINI" : "FALLBACK";
        String fullText = aiTextSanitizer.sanitizeResumeText(record.getFullOptimizedResume());
        if (!StringUtils.hasText(fullText)) fullText = "No full optimized resume generated yet.";

        return new ResumeEnhanceAiResult(
                limitSentence(aiTextSanitizer.sanitizePlainText(defaultIfEmpty(record.getAiSummary(), "Structured diagnosis completed.")), 140),
                coreProblems,
                aiTextSanitizer.sanitizePlainText(buildMatchingOverview(record.getReviewScore(), userProfile, jobPost)),
                missingKeywords,
                improvementSuggestions,
                education, skills, projects, experience,
                fullText,
                reviewSource,
                defaultIfEmpty(record.getReviewModelName(), aiFacadeService.getConfiguredModelName()),
                aiTextSanitizer.sanitizePlainText(record.getReviewFallbackReason()),
                aiUsed,
                formatCreatedAt(record.getCreatedAt())
        );
    }

    private String parsePdf(MultipartFile pdfFile) {
        String filename = normalizeText(pdfFile.getOriginalFilename()).toLowerCase(Locale.ROOT);
        if (!filename.endsWith(".pdf")) throw new IllegalArgumentException("Uploaded file is not PDF.");
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfFile.getBytes()))) {
            return aiTextSanitizer.sanitizeResumeText(new PDFTextStripper().getText(document));
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to parse PDF.");
        }
    }

    private ResumeParsedData splitResume(String text, String inputType) {
        String[] lines = text.split("\\r?\\n");
        StringBuilder basic = new StringBuilder();
        StringBuilder education = new StringBuilder();
        StringBuilder skills = new StringBuilder();
        StringBuilder projects = new StringBuilder();
        StringBuilder experience = new StringBuilder();
        StringBuilder others = new StringBuilder();
        String current = "basicInfo";
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (!StringUtils.hasText(line)) continue;
            if (containsAny(line, "education", "教育", "学历")) current = "education";
            else if (containsAny(line, "skills", "技能", "技术栈", "能力")) current = "skills";
            else if (containsAny(line, "project", "项目")) current = "projects";
            else if (containsAny(line, "intern", "实习", "校园", "工作经历", "实践经历")) current = "experience";
            else if (containsAny(line, "other", "其他", "自我评价", "补充")) current = "others";
            appendLine(resolveBuilder(current, basic, education, skills, projects, experience, others), line);
        }
        if (!StringUtils.hasText(basic.toString())) basic.append(extractPrefix(text, 260));
        if (!StringUtils.hasText(education.toString())) education.append("No education section identified.");
        if (!StringUtils.hasText(skills.toString())) skills.append("No skills section identified.");
        if (!StringUtils.hasText(projects.toString())) projects.append("No projects section identified.");
        if (!StringUtils.hasText(experience.toString())) experience.append("No internship/campus section identified.");
        if (!StringUtils.hasText(others.toString())) others.append("No extra section identified.");
        return new ResumeParsedData(
                aiTextSanitizer.sanitizeResumeText(text),
                inputType,
                aiTextSanitizer.sanitizeResumeText(basic.toString()),
                aiTextSanitizer.sanitizeResumeText(education.toString()),
                aiTextSanitizer.sanitizeResumeText(skills.toString()),
                aiTextSanitizer.sanitizeResumeText(projects.toString()),
                aiTextSanitizer.sanitizeResumeText(experience.toString()),
                aiTextSanitizer.sanitizeResumeText(others.toString())
        );
    }

    private StringBuilder resolveBuilder(String sectionKey, StringBuilder basic, StringBuilder education, StringBuilder skills,
                                         StringBuilder projects, StringBuilder experience, StringBuilder others) {
        return switch (sectionKey) {
            case "education" -> education;
            case "skills" -> skills;
            case "projects" -> projects;
            case "experience" -> experience;
            case "others" -> others;
            default -> basic;
        };
    }

    private String getSectionText(ResumeEnhanceRecord record, String sectionKey) {
        return switch (sectionKey) {
            case "basicInfo" -> sectionValue(record.getOptimizedBasicInfo(), record.getParsedBasicInfo());
            case "education" -> sectionValue(record.getOptimizedEducation(), record.getParsedEducation());
            case "skills" -> sectionValue(record.getOptimizedSkills(), record.getParsedSkills());
            case "projects" -> sectionValue(record.getOptimizedProjects(), record.getParsedProjects());
            case "experience" -> sectionValue(record.getOptimizedExperience(), record.getParsedExperience());
            case "others" -> sectionValue(record.getOptimizedOthers(), record.getParsedOthers());
            default -> "";
        };
    }

    private void setOptimizedSection(ResumeEnhanceRecord record, String sectionKey, String optimizedText) {
        switch (sectionKey) {
            case "basicInfo" -> record.setOptimizedBasicInfo(optimizedText);
            case "education" -> record.setOptimizedEducation(optimizedText);
            case "skills" -> record.setOptimizedSkills(optimizedText);
            case "projects" -> record.setOptimizedProjects(optimizedText);
            case "experience" -> record.setOptimizedExperience(optimizedText);
            case "others" -> record.setOptimizedOthers(optimizedText);
            default -> {
            }
        }
    }

    private void setSectionMetadata(ResumeEnhanceRecord record, String sectionKey, AiGenerationMetadata metadata) {
        String source = metadata.getGenerationSource().name();
        String reason = composeFallbackReason(metadata);
        String modelName = metadata.getModelName();
        switch (sectionKey) {
            case "basicInfo" -> { record.setBasicInfoGenerationSource(source); record.setBasicInfoFallbackReason(reason); record.setBasicInfoModelName(modelName); }
            case "education" -> { record.setEducationGenerationSource(source); record.setEducationFallbackReason(reason); record.setEducationModelName(modelName); }
            case "skills" -> { record.setSkillsGenerationSource(source); record.setSkillsFallbackReason(reason); record.setSkillsModelName(modelName); }
            case "projects" -> { record.setProjectsGenerationSource(source); record.setProjectsFallbackReason(reason); record.setProjectsModelName(modelName); }
            case "experience" -> { record.setExperienceGenerationSource(source); record.setExperienceFallbackReason(reason); record.setExperienceModelName(modelName); }
            case "others" -> { record.setOthersGenerationSource(source); record.setOthersFallbackReason(reason); record.setOthersModelName(modelName); }
            default -> { }
        }
    }

    private void setFullResumeMetadata(ResumeEnhanceRecord record, AiGenerationMetadata metadata) {
        record.setFullResumeGenerationSource(metadata.getGenerationSource().name());
        record.setFullResumeFallbackReason(composeFallbackReason(metadata));
        record.setFullResumeModelName(metadata.getModelName());
    }

    private String normalizeSectionKey(String sectionKey) {
        if (!StringUtils.hasText(sectionKey)) return "";
        String key = sectionKey.trim();
        if (containsAny(key, "basic", "base", "基础", "信息")) return "basicInfo";
        if (containsAny(key, "education", "edu", "教育")) return "education";
        if (containsAny(key, "skills", "skill", "技能")) return "skills";
        if (containsAny(key, "project", "项目")) return "projects";
        if (containsAny(key, "experience", "实习", "校园")) return "experience";
        if (containsAny(key, "other", "其他")) return "others";
        return key;
    }

    private String displaySectionName(String sectionKey) {
        return switch (sectionKey) {
            case "basicInfo" -> "basic info";
            case "education" -> "education";
            case "skills" -> "skills";
            case "projects" -> "projects";
            case "experience" -> "experience";
            case "others" -> "others";
            default -> "resume section";
        };
    }

    private String sectionValue(String preferred, String fallback) {
        if (StringUtils.hasText(preferred)) {
            ResumeRewriteBlock block = parseStoredRewriteBlock(preferred);
            if (block != null && StringUtils.hasText(block.getRewritten())) {
                return aiTextSanitizer.sanitizeResumeText(block.getRewritten());
            }
            return aiTextSanitizer.sanitizeResumeText(preferred);
        }
        return aiTextSanitizer.sanitizeResumeText(fallback);
    }

    private String serializeRewriteBlock(String original, String rewritten, String reason) {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("original", aiTextSanitizer.sanitizeResumeText(original));
        payload.put("rewritten", aiTextSanitizer.sanitizeResumeText(rewritten));
        payload.put("reason", aiTextSanitizer.sanitizePlainText(reason));
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception ex) {
            return aiTextSanitizer.sanitizeResumeText(rewritten);
        }
    }

    private ResumeRewriteBlock parseStoredRewriteBlock(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        String text = raw.trim();
        if (!text.startsWith("{") || !text.endsWith("}")) return null;
        try {
            JsonNode root = OBJECT_MAPPER.readTree(text);
            if (!root.isObject()) return null;
            return new ResumeRewriteBlock(readJsonText(root, "original"), readJsonText(root, "rewritten"), readJsonText(root, "reason"));
        } catch (Exception ex) {
            return null;
        }
    }

    private ResumeRewriteBlock toRewriteBlock(String originalText, String storedOptimized, String sectionTitle,
                                              String source, String fallbackReason, JobPost jobPost) {
        String original = defaultIfEmpty(aiTextSanitizer.sanitizeResumeText(originalText), "No source text.");
        ResumeRewriteBlock parsed = parseStoredRewriteBlock(storedOptimized);
        String rewritten = parsed != null
                ? defaultIfEmpty(aiTextSanitizer.sanitizeResumeText(parsed.getRewritten()), original)
                : defaultIfEmpty(aiTextSanitizer.sanitizeResumeText(storedOptimized), original);
        String reason = parsed != null ? aiTextSanitizer.sanitizePlainText(parsed.getReason()) : "";
        if (!StringUtils.hasText(reason)) reason = defaultRewriteReason(sectionTitle, source, fallbackReason, jobPost);
        return new ResumeRewriteBlock(original, rewritten, reason);
    }

    private String defaultRewriteReason(String sectionTitle, String source, String fallbackReason, JobPost jobPost) {
        if ("GEMINI".equalsIgnoreCase(normalizeText(source))) {
            return "Optimized for target role: " + defaultIfEmpty(jobPost.getJobName(), "target role");
        }
        if ("QWEN".equalsIgnoreCase(normalizeText(source))) {
            return "Optimized by Qwen for target role: " + defaultIfEmpty(jobPost.getJobName(), "target role");
        }
        if ("FALLBACK".equalsIgnoreCase(normalizeText(source))) {
            return "Generated by fallback rules."
                    + (StringUtils.hasText(fallbackReason) ? " Reason: " + aiTextSanitizer.sanitizePlainText(fallbackReason) : "");
        }
        return "No rewrite generated for " + sectionTitle + ".";
    }

    private List<String> parseJsonArrayOrSplit(String raw, int limit) {
        Set<String> result = new LinkedHashSet<>();
        if (!StringUtils.hasText(raw)) return new ArrayList<>();
        String text = raw.trim();
        if (text.startsWith("[") && text.endsWith("]")) {
            try {
                JsonNode node = OBJECT_MAPPER.readTree(text);
                if (node.isArray()) {
                    for (JsonNode item : node) {
                        String value = item == null ? "" : item.asText("").trim();
                        if (isValidItem(value)) result.add(value);
                        if (result.size() >= limit) return new ArrayList<>(result);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (result.isEmpty()) {
            for (String part : text.split("[,，。；;\\n]")) {
                String cleaned = trimListPrefix(part);
                if (isValidItem(cleaned)) result.add(cleaned);
                if (result.size() >= limit) break;
            }
        }
        return new ArrayList<>(result);
    }

    private List<String> splitToList(String text, int limit) {
        Set<String> items = new LinkedHashSet<>();
        if (!StringUtils.hasText(text)) return new ArrayList<>();
        String normalized = text.replace("。", ",").replace("，", ",").replace("；", ",").replace(";", ",").replace("\n", ",");
        for (String part : normalized.split(",")) {
            String cleaned = trimListPrefix(part);
            if (isValidItem(cleaned)) items.add(cleaned);
            if (items.size() >= limit) break;
        }
        return new ArrayList<>(items);
    }

    private List<String> mergeUnique(List<String> left, List<String> right, int limit) {
        Set<String> merged = new LinkedHashSet<>();
        if (left != null) merged.addAll(left);
        if (right != null) merged.addAll(right);
        List<String> result = new ArrayList<>();
        for (String item : merged) {
            String cleaned = trimListPrefix(item);
            if (!isValidItem(cleaned)) continue;
            result.add(cleaned);
            if (result.size() >= limit) break;
        }
        return result;
    }

    private List<String> ensureSuggestionCount(List<String> suggestions, UserProfile userProfile, JobPost jobPost) {
        List<String> result = new ArrayList<>(suggestions);
        List<String> defaults = List.of(
                "Use action-result metrics in project bullets.",
                "Integrate missing keywords into skills and projects.",
                "Align skills wording with target role direction."
        );
        for (String item : defaults) {
            if (result.size() >= MAX_SUGGESTIONS) break;
            if (!result.contains(item)) result.add(item);
        }
        if (!directionAligned(userProfile, jobPost) && result.size() < MAX_SUGGESTIONS) {
            result.add("Add a transition explanation for target role fit.");
        }
        if (result.size() < 3) result.add("Rewrite key sections before generating full resume.");
        return sanitizeListItems(result, MAX_SUGGESTIONS);
    }

    private String buildMatchingOverview(Integer score, UserProfile userProfile, JobPost jobPost) {
        int safeScore = score == null ? 0 : score;
        String level = safeScore >= 85 ? "high match" : (safeScore >= 70 ? "medium-high match" : (safeScore >= 55 ? "medium match" : "needs improvement"));
        String direction = directionAligned(userProfile, jobPost) ? "direction aligned" : "direction gap exists";
        return "Current fit: " + level + " (" + safeScore + "). " + direction + ".";
    }

    private boolean directionAligned(UserProfile userProfile, JobPost jobPost) {
        String profileDirection = normalizeText(userProfile.getTargetDirection()).toLowerCase(Locale.ROOT);
        String jobDirection = normalizeText(jobPost.getJobDirection()).toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(profileDirection) || !StringUtils.hasText(jobDirection)) return true;
        return profileDirection.contains(jobDirection) || jobDirection.contains(profileDirection);
    }

    private String writeJsonArray(List<String> items) {
        try { return OBJECT_MAPPER.writeValueAsString(items == null ? List.of() : items); }
        catch (Exception ex) { return "[]"; }
    }

    private String formatCreatedAt(LocalDateTime createdAt) {
        return createdAt == null ? "N/A" : createdAt.format(TIME_FORMATTER);
    }

    private String readJsonText(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) return "";
        return node.get(field).asText("").trim();
    }

    private String limitSentence(String text, int maxLength) {
        String normalized = normalizeText(text);
        if (!StringUtils.hasText(normalized)) return normalized;
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private String extractPrefix(String text, int maxLength) {
        String normalized = normalizeText(text);
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private void appendLine(StringBuilder builder, String line) {
        if (!StringUtils.hasText(line)) return;
        if (!builder.isEmpty()) builder.append('\n');
        builder.append(line);
    }

    private boolean isValidItem(String text) {
        return StringUtils.hasText(text) && text.trim().length() >= 2 && text.trim().length() <= 120;
    }

    private boolean containsAny(String source, String... keys) {
        String text = normalizeText(source).toLowerCase(Locale.ROOT);
        for (String key : keys) if (text.contains(normalizeText(key).toLowerCase(Locale.ROOT))) return true;
        return false;
    }

    private String trimListPrefix(String text) {
        return normalizeText(text).replaceFirst("^[\\-•*\\d\\s\\.、\\)]+", "").trim();
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replace("\u0000", "").replace("\r", "").trim();
    }

    private String defaultIfEmpty(String text, String fallback) {
        return StringUtils.hasText(text) ? text.trim() : fallback;
    }

    private List<String> sanitizeListItems(List<String> items, int limit) {
        return aiTextSanitizer.sanitizeListItems(items, limit);
    }

    private String composeFallbackReason(AiGenerationMetadata metadata) {
        String fallback = aiTextSanitizer.sanitizePlainText(metadata.getFallbackReason());
        String primary = aiTextSanitizer.sanitizePlainText(metadata.getPrimaryFailureReason());
        String secondary = aiTextSanitizer.sanitizePlainText(metadata.getSecondaryFailureReason());
        boolean timeout = metadata.isTimeoutOccurred();

        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(fallback)) {
            builder.append(fallback);
        }
        if (StringUtils.hasText(primary)) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append("Gemini(")
                    .append(metadata.getPrimaryFailureLayer())
                    .append("/")
                    .append(metadata.getPrimaryFailureType())
                    .append("): ")
                    .append(primary);
        }
        if (StringUtils.hasText(secondary)) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append("Qwen(")
                    .append(metadata.getSecondaryFailureLayer())
                    .append("/")
                    .append(metadata.getSecondaryFailureType())
                    .append("): ")
                    .append(secondary);
        }
        if (timeout) {
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append("timeout occurred");
        }
        return builder.toString();
    }
}
