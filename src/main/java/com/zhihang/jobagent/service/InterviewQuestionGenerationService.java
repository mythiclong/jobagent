package com.zhihang.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihang.jobagent.dto.AiInterviewQuestionDraft;
import com.zhihang.jobagent.dto.AiTextGenerationResult;
import com.zhihang.jobagent.dto.InterviewQuestionGenerationResult;
import com.zhihang.jobagent.entity.InterviewQuestion;
import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.entity.UserProfile;
import com.zhihang.jobagent.repository.InterviewQuestionRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class InterviewQuestionGenerationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[A-Za-z][A-Za-z0-9.+#/_-]{1,18}|[\\u4E00-\\u9FFF]{2,8}");
    private static final int GENERATION_TARGET = 12;
    private static final int MAX_ACCEPTED_COUNT = 8;
    private static final int MAX_PER_CATEGORY = 3;
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("自我介绍", "基础知识", "项目经历", "场景题", "追问题");
    private static final Set<String> GENERIC_HR_QUESTIONS = Set.of(
            "优点", "缺点", "职业规划", "为什么选择我们", "薪资期望", "加班", "抗压"
    );

    private final InterviewQuestionRepository interviewQuestionRepository;
    private final AiOrchestratorService aiOrchestratorService;
    private final ContentNormalizationService contentNormalizationService;

    public InterviewQuestionGenerationService(InterviewQuestionRepository interviewQuestionRepository,
                                              AiOrchestratorService aiOrchestratorService,
                                              ContentNormalizationService contentNormalizationService) {
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.aiOrchestratorService = aiOrchestratorService;
        this.contentNormalizationService = contentNormalizationService;
    }

    public InterviewQuestionGenerationResult generateQuestionsIfNeeded(UserProfile userProfile,
                                                                       JobPost jobPost,
                                                                       List<InterviewQuestion> existingQuestions,
                                                                       int minimumPoolSize) {
        List<InterviewQuestion> currentQuestions = existingQuestions == null ? List.of() : existingQuestions;
        if (currentQuestions.size() >= minimumPoolSize) {
            return InterviewQuestionGenerationResult.noAttempt();
        }

        QuestionContext context = buildContext(userProfile, jobPost, currentQuestions);
        AiTextGenerationResult aiResult = aiOrchestratorService.generateJson(
                buildPrompt(userProfile, jobPost, context, minimumPoolSize),
                "interview-question-generate",
                0.35f
        );

        if (!aiResult.getMetadata().isAiUsed() || !StringUtils.hasText(aiResult.getText())) {
            return InterviewQuestionGenerationResult.fallbackNotice("题目不足，已为你优先使用当前题库中的核心题目。");
        }

        List<AiInterviewQuestionDraft> drafts = parseDrafts(aiResult.getText());
        if (drafts.isEmpty()) {
            return InterviewQuestionGenerationResult.fallbackNotice("题目不足，已为你优先使用当前题库中的核心题目。");
        }

        List<InterviewQuestion> acceptedQuestions = acceptDrafts(
                drafts,
                context,
                aiResult.getSource().name()
        );
        if (acceptedQuestions.isEmpty()) {
            return InterviewQuestionGenerationResult.fallbackNotice("题目不足，已为你优先使用当前题库中的核心题目。");
        }

        List<InterviewQuestion> savedQuestions = interviewQuestionRepository.saveAll(acceptedQuestions);
        return new InterviewQuestionGenerationResult(
                savedQuestions,
                true,
                true,
                aiResult.getSource().name(),
                "已结合当前岗位自动补充相关题目，本轮优先为你整理更贴近岗位的问答。"
        );
    }

    private List<InterviewQuestion> acceptDrafts(List<AiInterviewQuestionDraft> drafts,
                                                 QuestionContext context,
                                                 String providerName) {
        List<AiInterviewQuestionDraft> orderedDrafts = new ArrayList<>(drafts);
        orderedDrafts.sort(Comparator.comparingInt((AiInterviewQuestionDraft draft) -> draft.getQuestion() == null ? 0 : draft.getQuestion().length()).reversed());

        Map<String, Integer> categoryCounter = new LinkedHashMap<>();
        Set<String> acceptedHashes = new LinkedHashSet<>();
        List<InterviewQuestion> acceptedQuestions = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        for (AiInterviewQuestionDraft draft : orderedDrafts) {
            AiInterviewQuestionDraft normalizedDraft = normalizeDraft(draft, context);
            if (!isUsableDraft(normalizedDraft, context)) {
                continue;
            }

            String category = normalizedDraft.getCategory();
            int categoryCount = categoryCounter.getOrDefault(category, 0);
            if (categoryCount >= maxPerCategory(category)) {
                continue;
            }

            String questionHash = buildQuestionHash(context.roleCategory(), category, normalizedDraft.getQuestion());
            if (!acceptedHashes.add(questionHash)) {
                continue;
            }
            if (interviewQuestionRepository.findByQuestionHash(questionHash).isPresent()) {
                continue;
            }

            InterviewQuestion question = buildQuestionEntity(normalizedDraft, context, providerName, questionHash, now);
            acceptedQuestions.add(question);
            categoryCounter.put(category, categoryCount + 1);
            if (acceptedQuestions.size() >= MAX_ACCEPTED_COUNT) {
                break;
            }
        }

        return acceptedQuestions;
    }

    private InterviewQuestion buildQuestionEntity(AiInterviewQuestionDraft draft,
                                                  QuestionContext context,
                                                  String providerName,
                                                  String questionHash,
                                                  LocalDateTime now) {
        InterviewQuestion question = new InterviewQuestion();
        question.setJobDirection(context.roleCategory());
        question.setRoleCategory(context.roleCategory());
        question.setQuestionType(draft.getCategory());
        question.setQuestionText(draft.getQuestion());
        question.setReferenceAnswer(String.join("\n", draft.getAnswerOutline()));
        question.setKeyPoints(String.join("\n", draft.getExpectedPoints()));
        question.setScoringRubric(String.join("\n", draft.getScoringRubric()));
        question.setSourceType("AI_GENERATED");
        question.setSourceName("AI_GENERATED");
        question.setProviderName(providerName);
        question.setQuestionHash(questionHash);
        question.setGeneratedAt(now);
        question.setUpdatedAt(now);
        question.setDisplayDifficulty(draft.getDifficulty());
        question.setDisplayTags(String.join(" / ", draft.getTags()));
        question.setRawTitle(draft.getQuestion());
        question.setRawContent(String.join("\n", combineDraftText(draft)));
        contentNormalizationService.normalizeInterviewQuestion(question);
        question.setDisplayDifficulty(draft.getDifficulty());
        question.setDisplayTags(String.join(" / ", draft.getTags()));
        return question;
    }

    private List<AiInterviewQuestionDraft> parseDrafts(String rawJsonText) {
        JsonNode root = parseJsonObject(rawJsonText);
        if (root == null || !root.isObject()) {
            return List.of();
        }

        JsonNode questionsNode = root.path("questions");
        if (!questionsNode.isArray()) {
            return List.of();
        }

        List<AiInterviewQuestionDraft> drafts = new ArrayList<>();
        for (JsonNode item : questionsNode) {
            if (item == null || !item.isObject()) {
                continue;
            }
            AiInterviewQuestionDraft draft = new AiInterviewQuestionDraft();
            draft.setQuestion(readText(item, "question"));
            draft.setCategory(readText(item, "category"));
            draft.setDifficulty(readText(item, "difficulty"));
            draft.setExpectedPoints(readArray(item, "expectedPoints", 6));
            draft.setAnswerOutline(readArray(item, "answerOutline", 6));
            draft.setScoringRubric(readArray(item, "scoringRubric", 6));
            draft.setTags(readArray(item, "tags", 6));
            drafts.add(draft);
        }
        return drafts;
    }

    private JsonNode parseJsonObject(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String raw = text.trim();
        try {
            JsonNode direct = OBJECT_MAPPER.readTree(raw);
            if (direct != null && direct.isObject()) {
                return direct;
            }
        } catch (Exception ignored) {
        }

        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            JsonNode embedded = OBJECT_MAPPER.readTree(raw.substring(start, end + 1));
            return embedded != null && embedded.isObject() ? embedded : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private String readText(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return "";
        }
        JsonNode valueNode = node.get(field);
        if (valueNode.isTextual() || valueNode.isNumber() || valueNode.isBoolean()) {
            return cleanPlainText(valueNode.asText(""));
        }
        return cleanPlainText(valueNode.toString());
    }

    private List<String> readArray(JsonNode node, String field, int limit) {
        Set<String> items = new LinkedHashSet<>();
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return new ArrayList<>();
        }
        JsonNode arrayNode = node.get(field);
        if (arrayNode.isArray()) {
            for (JsonNode item : arrayNode) {
                String text = item == null ? "" : cleanPlainText(item.asText(""));
                if (isValidListItem(text)) {
                    items.add(text);
                }
                if (items.size() >= limit) {
                    break;
                }
            }
            return new ArrayList<>(items);
        }

        String text = cleanPlainText(arrayNode.asText(""));
        for (String token : text.split("[,，；;、\\n]")) {
            String cleaned = cleanPlainText(token);
            if (isValidListItem(cleaned)) {
                items.add(cleaned);
            }
            if (items.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(items);
    }

    private AiInterviewQuestionDraft normalizeDraft(AiInterviewQuestionDraft draft, QuestionContext context) {
        AiInterviewQuestionDraft normalized = new AiInterviewQuestionDraft();
        normalized.setQuestion(normalizeQuestion(draft.getQuestion()));
        normalized.setCategory(normalizeCategory(draft.getCategory()));
        normalized.setDifficulty(normalizeDifficulty(draft.getDifficulty()));
        normalized.setExpectedPoints(normalizeList(draft.getExpectedPoints(), 5));
        normalized.setAnswerOutline(normalizeList(draft.getAnswerOutline(), 5));
        normalized.setScoringRubric(normalizeList(draft.getScoringRubric(), 5));

        List<String> mergedTags = new ArrayList<>(normalizeList(draft.getTags(), 6));
        mergedTags.add(context.roleCategory());
        mergedTags.addAll(pickTopTokens(String.join(" ", draft.getExpectedPoints()), 2));
        normalized.setTags(dedupe(mergedTags, 6));
        return normalized;
    }

    private boolean isUsableDraft(AiInterviewQuestionDraft draft, QuestionContext context) {
        if (!StringUtils.hasText(draft.getQuestion())
                || !StringUtils.hasText(draft.getCategory())
                || !StringUtils.hasText(draft.getDifficulty())) {
            return false;
        }
        if (!ALLOWED_CATEGORIES.contains(draft.getCategory())) {
            return false;
        }
        if (draft.getQuestion().length() < 10 || draft.getQuestion().length() > 90) {
            return false;
        }
        if (draft.getExpectedPoints().size() < 2 || draft.getAnswerOutline().size() < 2 || draft.getScoringRubric().size() < 2) {
            return false;
        }
        if (containsGenericHrQuestion(draft.getQuestion())) {
            return false;
        }

        String combinedText = String.join(" ", combineDraftText(draft)).toLowerCase(Locale.ROOT);
        Set<String> candidateTokens = tokenize(combinedText);
        if (candidateTokens.isEmpty()) {
            return false;
        }

        if (isHighlySimilarToExisting(candidateTokens, draft.getQuestion(), context)) {
            return false;
        }

        int focusHitCount = intersectCount(candidateTokens, context.focusTokens());
        boolean projectLikeCategory = "项目经历".equals(draft.getCategory()) || "追问题".equals(draft.getCategory());
        if (focusHitCount == 0 && !projectLikeCategory) {
            return false;
        }
        if ("自我介绍".equals(draft.getCategory()) && !combinedText.contains("项目") && !combinedText.contains("岗位")) {
            return false;
        }
        return true;
    }

    private boolean isHighlySimilarToExisting(Set<String> candidateTokens,
                                              String questionText,
                                              QuestionContext context) {
        String normalizedQuestion = normalizeForSimilarity(questionText);
        for (QuestionSignature signature : context.existingSignatures()) {
            if (normalizedQuestion.equals(signature.normalizedQuestion())) {
                return true;
            }
            if (signature.normalizedQuestion().contains(normalizedQuestion) || normalizedQuestion.contains(signature.normalizedQuestion())) {
                return true;
            }
            double similarity = jaccardSimilarity(candidateTokens, signature.tokens());
            if (similarity >= 0.84d) {
                return true;
            }
        }
        return false;
    }

    private String buildPrompt(UserProfile userProfile,
                               JobPost jobPost,
                               QuestionContext context,
                               int minimumPoolSize) {
        String existingQuestionHints = context.existingQuestionHints().isEmpty()
                ? "暂无"
                : String.join(" | ", context.existingQuestionHints());
        int expectedCount = Math.max(GENERATION_TARGET, minimumPoolSize + 2);
        return """
                你是岗位面试题库生成助手。只允许返回一个 JSON 对象，不要 markdown，不要额外解释。
                JSON 结构必须为：
                {
                  "roleCategory": "岗位方向",
                  "questions": [
                    {
                      "question": "中文题目",
                      "category": "自我介绍|基础知识|项目经历|场景题|追问题",
                      "difficulty": "基础|中等|进阶",
                      "expectedPoints": ["考察点1", "考察点2"],
                      "answerOutline": ["回答结构1", "回答结构2", "回答结构3"],
                      "scoringRubric": ["评分标准1", "评分标准2"],
                      "tags": ["标签1", "标签2"]
                    }
                  ]
                }

                生成要求：
                1. 共生成 %d 道中文题。
                2. 必须围绕目标岗位方向、岗位技能和项目/实习偏好，不要输出通用 HR 问题。
                3. 题型要有分布：自我介绍不超过 1 道，项目经历/场景题/追问题要有覆盖。
                4. 每道题都要有明确考察点、回答提纲和评分标准。
                5. 不要与已有题高度重复，不要输出 markdown、序号、代码块或空字段。

                目标岗位：
                - 岗位名称：%s
                - 岗位方向：%s
                - 技能要求：%s
                - 岗位性质：%s
                - 学历要求：%s

                候选人画像：
                - 目标方向：%s
                - 技能：%s
                - 项目经历：%s
                - 目标城市：%s

                重点关键词：%s
                参考已有题：%s
                """.formatted(
                expectedCount,
                safe(jobPost == null ? null : jobPost.getDisplayTitle()),
                context.roleCategory(),
                safe(jobPost == null ? null : jobPost.getJobRequirements()),
                safe(jobPost == null ? null : jobPost.getJobNature()),
                safe(jobPost == null ? null : jobPost.getEducationRequirement()),
                safe(userProfile == null ? null : userProfile.getTargetDirection()),
                safe(userProfile == null ? null : userProfile.getSkills()),
                safe(userProfile == null ? null : userProfile.getProjectExperience()),
                safe(userProfile == null ? null : userProfile.getExpectedCity()),
                String.join(" / ", context.promptKeywords()),
                existingQuestionHints
        );
    }

    private QuestionContext buildContext(UserProfile userProfile,
                                         JobPost jobPost,
                                         List<InterviewQuestion> existingQuestions) {
        String roleCategory = normalizeRoleCategory(
                safe(jobPost == null ? null : jobPost.getJobDirection()),
                safe(userProfile == null ? null : userProfile.getTargetDirection()),
                safe(jobPost == null ? null : jobPost.getDisplayTitle())
        );

        String jobFocusedText = String.join(" ",
                safe(jobPost == null ? null : jobPost.getJobDirection()),
                safe(jobPost == null ? null : jobPost.getJobRequirements()),
                safe(jobPost == null ? null : jobPost.getBonusPoints()),
                safe(jobPost == null ? null : jobPost.getJobDescription()),
                safe(jobPost == null ? null : jobPost.getDisplayTitle())
        );
        String profileSupportingText = String.join(" ",
                safe(userProfile == null ? null : userProfile.getSkills()),
                safe(userProfile == null ? null : userProfile.getProjectExperience())
        );
        String profileFallbackText = String.join(" ",
                safe(userProfile == null ? null : userProfile.getTargetDirection()),
                safe(userProfile == null ? null : userProfile.getSkills()),
                safe(userProfile == null ? null : userProfile.getProjectExperience()),
                safe(userProfile == null ? null : userProfile.getCareerGoal())
        );
        String combinedText = StringUtils.hasText(jobFocusedText)
                ? String.join(" ", jobFocusedText, profileSupportingText)
                : profileFallbackText;

        List<String> promptKeywords = dedupe(pickTopTokens(combinedText, 10), 10);
        Set<String> focusTokens = tokenize(roleCategory + " " + String.join(" ", promptKeywords));

        List<QuestionSignature> existingSignatures = new ArrayList<>();
        List<String> existingQuestionHints = new ArrayList<>();
        for (InterviewQuestion existingQuestion : existingQuestions) {
            String title = safe(existingQuestion.getDisplayTitle());
            if (!title.isEmpty()) {
                existingQuestionHints.add(title);
            }
            existingSignatures.add(new QuestionSignature(
                    normalizeForSimilarity(title),
                    tokenize(title + " " + safe(existingQuestion.getKeyPoints()) + " " + safe(existingQuestion.getDisplayTags()))
            ));
        }

        return new QuestionContext(
                roleCategory,
                promptKeywords,
                focusTokens,
                dedupe(existingQuestionHints, 8),
                existingSignatures
        );
    }

    private String normalizeRoleCategory(String... candidates) {
        String joined = String.join(" ", candidates).toLowerCase(Locale.ROOT);
        if (containsAny(joined, "后端", "backend", "java", "spring", "mysql", "微服务")) {
            return "Java后端";
        }
        if (containsAny(joined, "前端", "frontend", "react", "vue", "javascript", "typescript")) {
            return "前端开发";
        }
        if (containsAny(joined, "产品", "product", "prd", "原型", "需求")) {
            return "产品经理";
        }
        if (containsAny(joined, "测试", "qa", "自动化测试", "接口测试", "jmeter", "selenium")) {
            return "测试开发";
        }
        if (containsAny(joined, "数据", "data", "analysis", "python", "bi", "pandas")) {
            return "数据分析";
        }
        if (containsAny(joined, "运营", "operation", "growth", "用户增长", "活动运营")) {
            return "运营";
        }
        if (containsAny(joined, "设计", "design", "ui", "ux", "交互")) {
            return "设计";
        }
        return "通用岗位";
    }

    private String normalizeQuestion(String question) {
        String cleaned = cleanPlainText(question)
                .replace("**", "")
                .replace("```", "")
                .replace("#", "")
                .replaceFirst("^[\\-•*\\d\\.、]+", "")
                .trim();
        if (!StringUtils.hasText(cleaned)) {
            return "";
        }
        if (!cleaned.endsWith("？") && !cleaned.endsWith("?")) {
            cleaned = cleaned + "？";
        }
        return cleaned;
    }

    private String normalizeCategory(String category) {
        String value = cleanPlainText(category).toLowerCase(Locale.ROOT);
        if (containsAny(value, "自我介绍", "自我介绍题", "introduce")) {
            return "自我介绍";
        }
        if (containsAny(value, "项目", "project", "经历")) {
            return "项目经历";
        }
        if (containsAny(value, "场景", "scenario", "debug", "case")) {
            return "场景题";
        }
        if (containsAny(value, "追问", "follow", "深挖")) {
            return "追问题";
        }
        return "基础知识";
    }

    private String normalizeDifficulty(String difficulty) {
        String value = cleanPlainText(difficulty).toLowerCase(Locale.ROOT);
        if (containsAny(value, "进阶", "困难", "较难", "advanced", "hard")) {
            return "进阶";
        }
        if (containsAny(value, "中等", "标准", "medium", "normal")) {
            return "中等";
        }
        return "基础";
    }

    private List<String> normalizeList(List<String> items, int limit) {
        List<String> normalized = new ArrayList<>();
        if (items == null) {
            return normalized;
        }
        for (String item : items) {
            String cleaned = cleanPlainText(item)
                    .replace("**", "")
                    .replace("```", "")
                    .replace("#", "")
                    .replaceFirst("^[\\-•*\\d\\.、]+", "")
                    .trim();
            if (!isValidListItem(cleaned)) {
                continue;
            }
            normalized.add(cleaned);
            if (normalized.size() >= limit) {
                break;
            }
        }
        return dedupe(normalized, limit);
    }

    private List<String> combineDraftText(AiInterviewQuestionDraft draft) {
        List<String> parts = new ArrayList<>();
        parts.add(draft.getQuestion());
        parts.addAll(draft.getTags());
        parts.addAll(draft.getExpectedPoints());
        parts.addAll(draft.getAnswerOutline());
        parts.addAll(draft.getScoringRubric());
        return parts;
    }

    private List<String> pickTopTokens(String text, int limit) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = TOKEN_PATTERN.matcher(cleanPlainText(text));
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (!isUsefulToken(token)) {
                continue;
            }
            tokens.add(token);
            if (tokens.size() >= limit * 2) {
                break;
            }
        }
        return dedupe(tokens, limit);
    }

    private boolean isUsefulToken(String token) {
        String lower = token.toLowerCase(Locale.ROOT);
        if (token.length() <= 1) {
            return false;
        }
        return !containsAny(lower, "岗位", "要求", "熟悉", "了解", "负责", "具备", "相关", "能力", "经验");
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        Matcher matcher = TOKEN_PATTERN.matcher(cleanPlainText(text).toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String token = matcher.group().trim();
            if (isUsefulToken(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private String normalizeForSimilarity(String text) {
        return cleanPlainText(text)
                .toLowerCase(Locale.ROOT)
                .replace("？", "")
                .replace("?", "")
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4E00-\\u9FFF]+", "");
    }

    private double jaccardSimilarity(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0d;
        }
        Set<String> intersection = new LinkedHashSet<>(left);
        intersection.retainAll(right);
        Set<String> union = new LinkedHashSet<>(left);
        union.addAll(right);
        return union.isEmpty() ? 0d : (double) intersection.size() / union.size();
    }

    private int intersectCount(Set<String> left, Set<String> right) {
        int count = 0;
        for (String token : left) {
            if (right.contains(token)) {
                count++;
            }
        }
        return count;
    }

    private String buildQuestionHash(String roleCategory, String category, String questionText) {
        String payload = normalizeForSimilarity(roleCategory + "|" + category + "|" + questionText);
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            return Integer.toHexString(payload.hashCode());
        }
    }

    private int maxPerCategory(String category) {
        if ("自我介绍".equals(category)) {
            return 1;
        }
        if ("追问题".equals(category)) {
            return 2;
        }
        return MAX_PER_CATEGORY;
    }

    private boolean containsGenericHrQuestion(String questionText) {
        String normalized = questionText.toLowerCase(Locale.ROOT);
        for (String phrase : GENERIC_HR_QUESTIONS) {
            if (normalized.contains(phrase.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidListItem(String text) {
        return StringUtils.hasText(text) && text.length() >= 2 && text.length() <= 80;
    }

    private List<String> dedupe(List<String> values, int limit) {
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            if (!StringUtils.hasText(value)) {
                continue;
            }
            result.add(value.trim());
            if (result.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(result);
    }

    private String cleanPlainText(String text) {
        return contentNormalizationService.cleanImportedText(text == null ? "" : text)
                .replace('\u3000', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String safe(String text) {
        return text == null ? "" : text.trim();
    }

    private record QuestionSignature(String normalizedQuestion, Set<String> tokens) {
    }

    private record QuestionContext(String roleCategory,
                                   List<String> promptKeywords,
                                   Set<String> focusTokens,
                                   List<String> existingQuestionHints,
                                   List<QuestionSignature> existingSignatures) {
    }
}
