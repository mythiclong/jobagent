package com.zhihang.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihang.jobagent.dto.AiFailureLayer;
import com.zhihang.jobagent.dto.AiFailureType;
import com.zhihang.jobagent.dto.AiGenerationMetadata;
import com.zhihang.jobagent.dto.AiTextGenerationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AiOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestratorService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final GeminiAiProvider geminiAiProvider;
    private final QwenAiProvider qwenAiProvider;

    public AiOrchestratorService(GeminiAiProvider geminiAiProvider, QwenAiProvider qwenAiProvider) {
        this.geminiAiProvider = geminiAiProvider;
        this.qwenAiProvider = qwenAiProvider;
    }

    public AiTextGenerationResult generateText(String prompt, String action, float temperature) {
        return generate(prompt, action, temperature, false);
    }

    public AiTextGenerationResult generateJson(String prompt, String action, float temperature) {
        return generate(prompt, action, temperature, true);
    }

    public boolean isGeminiConfigured() {
        return geminiAiProvider.isEnabled();
    }

    public String getGeminiModelName() {
        return geminiAiProvider.getConfiguredModelName();
    }

    public String getGeminiKeySource() {
        return geminiAiProvider.getApiKeySource();
    }

    public int getGeminiTimeoutSeconds() {
        return geminiAiProvider.getTimeoutSeconds();
    }

    public boolean isQwenConfigured() {
        return qwenAiProvider.isEnabled();
    }

    public String getQwenModelName() {
        return qwenAiProvider.getConfiguredModelName();
    }

    public String getQwenKeySource() {
        return qwenAiProvider.getApiKeySource();
    }

    public int getQwenTimeoutSeconds() {
        return qwenAiProvider.getTimeoutSeconds();
    }

    private AiTextGenerationResult generate(String prompt, String action, float temperature, boolean expectJsonObject) {
        log.info("AI orchestrator: action={}, step=gemini_start", action);
        AiProviderResult geminiResult = geminiAiProvider.generate(action, prompt, temperature);
        if (isUsable(geminiResult, expectJsonObject)) {
            log.info("AI orchestrator: action={}, step=gemini_success, model={}", action, geminiResult.getModelName());
            return new AiTextGenerationResult(
                    geminiResult.getContent(),
                    AiGenerationMetadata.gemini(geminiResult.getModelName())
            );
        }

        FailureDetail primaryFailure = normalizeFailure(geminiResult, expectJsonObject);
        log.warn("AI orchestrator: action={}, step=gemini_failed, layer={}, type={}, reason={}",
                action, primaryFailure.layer(), primaryFailure.type(), primaryFailure.reason());

        log.info("AI orchestrator: action={}, step=qwen_start", action);
        AiProviderResult qwenResult = qwenAiProvider.generate(action, prompt, temperature);
        if (isUsable(qwenResult, expectJsonObject)) {
            log.info("AI orchestrator: action={}, step=qwen_success, model={}", action, qwenResult.getModelName());
            return new AiTextGenerationResult(
                    qwenResult.getContent(),
                    AiGenerationMetadata.qwen(
                            qwenResult.getModelName(),
                            primaryFailure.reason(),
                            primaryFailure.layer(),
                            primaryFailure.type()
                    )
            );
        }

        FailureDetail secondaryFailure = normalizeFailure(qwenResult, expectJsonObject);
        log.warn("AI orchestrator: action={}, step=qwen_failed, layer={}, type={}, reason={}",
                action, secondaryFailure.layer(), secondaryFailure.type(), secondaryFailure.reason());
        boolean timeoutOccurred = geminiResult.isTimeoutOccurred() || qwenResult.isTimeoutOccurred();
        String fallbackReason = "Gemini and Qwen are unavailable. Switched to local fallback.";
        log.warn("AI orchestrator: action={}, step=fallback, geminiReason={}, qwenReason={}",
                action, primaryFailure.reason(), secondaryFailure.reason());

        return new AiTextGenerationResult(
                "",
                AiGenerationMetadata.fallback(
                        "local-rule",
                        fallbackReason,
                        primaryFailure.reason(),
                        secondaryFailure.reason(),
                        timeoutOccurred,
                        primaryFailure.layer(),
                        primaryFailure.type(),
                        secondaryFailure.layer(),
                        secondaryFailure.type()
                )
        );
    }

    private boolean isUsable(AiProviderResult result, boolean expectJsonObject) {
        if (result == null || !result.isSuccess()) {
            return false;
        }
        String content = defaultText(result.getContent());
        if (!StringUtils.hasText(content)) {
            return false;
        }
        if (!expectJsonObject) {
            return true;
        }
        return extractJsonObject(content) != null;
    }

    private FailureDetail normalizeFailure(AiProviderResult result, boolean expectJsonObject) {
        if (result == null) {
            return new FailureDetail(
                    "Provider returned null result.",
                    AiFailureLayer.UNKNOWN,
                    AiFailureType.UNKNOWN_EXCEPTION
            );
        }
        if (!result.isSuccess()) {
            return new FailureDetail(
                    defaultText(result.getFailureReason()),
                    result.getFailureLayer(),
                    result.getFailureType()
            );
        }
        if (!StringUtils.hasText(result.getContent())) {
            return new FailureDetail(
                    result.getSource() + " returned empty content.",
                    AiFailureLayer.MODEL,
                    AiFailureType.RESPONSE_EMPTY
            );
        }
        if (expectJsonObject && extractJsonObject(result.getContent()) == null) {
            return new FailureDetail(
                    result.getSource() + " returned non-JSON response.",
                    AiFailureLayer.MODEL,
                    AiFailureType.RESPONSE_PARSE_FAILED
            );
        }
        return new FailureDetail(
                "Unknown provider failure.",
                AiFailureLayer.UNKNOWN,
                AiFailureType.UNKNOWN_EXCEPTION
        );
    }

    private JsonNode extractJsonObject(String text) {
        String raw = defaultText(text);
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        String candidate = raw.substring(start, end + 1).trim();
        try {
            JsonNode node = OBJECT_MAPPER.readTree(candidate);
            return node != null && node.isObject() ? node : null;
        } catch (Exception ex) {
            return null;
        }
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }

    private record FailureDetail(String reason, AiFailureLayer layer, AiFailureType type) {
    }
}
