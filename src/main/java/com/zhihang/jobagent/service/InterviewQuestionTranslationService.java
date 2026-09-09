package com.zhihang.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihang.jobagent.dto.AiTextGenerationResult;
import com.zhihang.jobagent.entity.InterviewQuestion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class InterviewQuestionTranslationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AiOrchestratorService aiOrchestratorService;
    private final ContentNormalizationService contentNormalizationService;
    private final boolean enabled;
    private final int batchSize;

    public InterviewQuestionTranslationService(AiOrchestratorService aiOrchestratorService,
                                               ContentNormalizationService contentNormalizationService,
                                               @Value("${jobagent.ai.question-title-translation.enabled:true}") boolean enabled,
                                               @Value("${jobagent.ai.question-title-translation.batch-size:12}") int batchSize) {
        this.aiOrchestratorService = aiOrchestratorService;
        this.contentNormalizationService = contentNormalizationService;
        this.enabled = enabled;
        this.batchSize = Math.max(1, batchSize);
    }

    public void translateTitlesIfNeeded(List<InterviewQuestion> questions) {
        if (!enabled
                || questions == null
                || questions.isEmpty()
                || (!aiOrchestratorService.isGeminiConfigured() && !aiOrchestratorService.isQwenConfigured())) {
            return;
        }

        List<TranslationCandidate> candidates = new ArrayList<>();
        for (InterviewQuestion question : questions) {
            if (shouldTranslate(question)) {
                candidates.add(new TranslationCandidate(question, contentNormalizationService.cleanImportedText(question.getRawTitle())));
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        for (int start = 0; start < candidates.size(); start += batchSize) {
            int end = Math.min(candidates.size(), start + batchSize);
            List<TranslationCandidate> batch = candidates.subList(start, end);
            Map<Integer, String> translations = requestTranslations(batch);
            for (int index = 0; index < batch.size(); index++) {
                String translatedTitle = translations.get(index);
                if (StringUtils.hasText(translatedTitle)) {
                    batch.get(index).question().setDisplayTitle(translatedTitle);
                }
            }
        }
    }

    private boolean shouldTranslate(InterviewQuestion question) {
        if (question == null) {
            return false;
        }
        String rawTitle = contentNormalizationService.cleanImportedText(question.getRawTitle());
        if (!looksLikeEnglishQuestion(rawTitle)) {
            return false;
        }
        if (!question.isOrganized()) {
            return true;
        }

        String displayTitle = contentNormalizationService.cleanImportedText(question.getDisplayTitle());
        return !looksLocalized(displayTitle) || looksBrokenMixedTitle(displayTitle);
    }

    private Map<Integer, String> requestTranslations(List<TranslationCandidate> batch) {
        AiTextGenerationResult result = aiOrchestratorService.generateJson(
                buildPrompt(batch),
                "interview-question-translate",
                0.1f
        );
        if (!result.getMetadata().isAiUsed() || !StringUtils.hasText(result.getText())) {
            return Map.of();
        }

        JsonNode root = parseJsonObject(result.getText());
        if (root == null || !root.path("translations").isArray()) {
            return Map.of();
        }

        Map<Integer, String> translations = new LinkedHashMap<>();
        for (JsonNode item : root.path("translations")) {
            if (item == null || !item.isObject()) {
                continue;
            }
            int index = item.path("index").asInt(-1);
            if (index < 0 || index >= batch.size()) {
                continue;
            }
            String translatedTitle = normalizeTranslatedTitle(item.path("title").asText(""));
            if (StringUtils.hasText(translatedTitle)) {
                translations.put(index, translatedTitle);
            }
        }
        return translations;
    }

    private String buildPrompt(List<TranslationCandidate> batch) {
        StringBuilder itemsBuilder = new StringBuilder();
        for (int index = 0; index < batch.size(); index++) {
            TranslationCandidate candidate = batch.get(index);
            if (!itemsBuilder.isEmpty()) {
                itemsBuilder.append('\n');
            }
            itemsBuilder.append(index)
                    .append(". [")
                    .append(defaultText(candidate.question().getJobDirection()))
                    .append("] ")
                    .append(candidate.rawTitle());
        }

        return """
                你是技术面试题翻译助手。请把英文面试题标题翻译成自然、简洁、适合中文校招/实习场景的中文问句。
                要求：
                1. 只翻译，不扩写，不解释。
                2. 保留技术术语和专有名词，例如 Cookie、Promise、Redis、Spring Boot。
                3. 去掉英文序号、无意义前导符号和残缺片段。
                4. 如果原题是问句，输出也必须是中文问句。
                5. 只返回一个 JSON 对象，不要 markdown。
                6. JSON 结构必须为：
                {
                  "translations": [
                    {"index": 0, "title": "中文题目"}
                  ]
                }

                待翻译列表：
                %s
                """.formatted(itemsBuilder);
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

    private String normalizeTranslatedTitle(String title) {
        String cleaned = contentNormalizationService.cleanImportedText(title)
                .replaceFirst("^[\\p{Punct}·•、，。；：！？【】（）《》“”‘’]+\\s*", "")
                .trim();
        if (!StringUtils.hasText(cleaned) || !looksLocalized(cleaned)) {
            return "";
        }
        if (cleaned.endsWith("?")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1) + "？";
        } else if (!cleaned.endsWith("？")) {
            cleaned = cleaned + "？";
        }
        return cleaned;
    }

    private boolean looksLikeEnglishQuestion(String title) {
        String lower = defaultText(title).toLowerCase(Locale.ROOT);
        if (!lower.matches(".*[a-z].*")) {
            return false;
        }
        return lower.startsWith("what ")
                || lower.startsWith("why ")
                || lower.startsWith("how ")
                || lower.startsWith("when ")
                || lower.startsWith("where ")
                || lower.startsWith("which ")
                || lower.startsWith("who ")
                || lower.startsWith("explain ")
                || lower.startsWith("describe ")
                || lower.endsWith("?");
    }

    private boolean looksLocalized(String title) {
        String value = defaultText(title);
        if (!value.matches(".*[\\u4E00-\\u9FFF].*")) {
            return false;
        }
        return !looksBrokenMixedTitle(value);
    }

    private boolean looksBrokenMixedTitle(String title) {
        String lower = defaultText(title).toLowerCase(Locale.ROOT);
        if (!lower.matches(".*[\\u4E00-\\u9FFF].*") || !lower.matches(".*[a-z].*")) {
            return false;
        }
        return lower.matches(".*\\b(what|why|how|when|where|which|who|do|does|did|you|we|need|use|access)\\b.*");
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }

    private record TranslationCandidate(InterviewQuestion question, String rawTitle) {
    }
}
