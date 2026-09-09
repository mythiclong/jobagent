package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.dto.AiFailureLayer;
import com.zhihang.jobagent.dto.AiFailureType;
import com.zhihang.jobagent.dto.AiGenerationMetadata;
import com.zhihang.jobagent.dto.AiTextGenerationResult;
import com.zhihang.jobagent.dto.GenerationSource;
import com.zhihang.jobagent.service.AiFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.LinkedHashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin/ai-test")
public class AdminAiTestController {

    private static final Logger log = LoggerFactory.getLogger(AdminAiTestController.class);
    private static final String DEFAULT_TEST_TEXT = "Please reply with: AI_TEST_OK";

    private final AiFacadeService aiFacadeService;

    public AdminAiTestController(AiFacadeService aiFacadeService) {
        this.aiFacadeService = aiFacadeService;
    }

    @GetMapping
    public String showPage(Model model) {
        fillBaseModel(model);
        model.addAttribute("inputText", DEFAULT_TEST_TEXT);
        model.addAttribute("tested", false);
        model.addAttribute("success", false);
        model.addAttribute("resultText", "");
        model.addAttribute("resultMetadata", AiGenerationMetadata.fallback("local-rule", ""));
        model.addAttribute("finalSourceLabel", sourceLabel(GenerationSource.FALLBACK));
        return "admin/ai-test";
    }

    @PostMapping
    public String testConnection(@RequestParam(required = false) String inputText, Model model) {
        String normalizedInput = StringUtils.hasText(inputText) ? inputText.trim() : DEFAULT_TEST_TEXT;
        fillBaseModel(model);
        model.addAttribute("inputText", normalizedInput);
        model.addAttribute("tested", true);

        TestResponse response = runTest(normalizedInput);
        model.addAttribute("success", response.success());
        model.addAttribute("resultText", response.text());
        model.addAttribute("resultMetadata", response.metadata());
        model.addAttribute("finalSourceLabel", response.sourceLabel());
        return "admin/ai-test";
    }

    @PostMapping("/run")
    @ResponseBody
    public Map<String, Object> runTestJson(@RequestParam(required = false) String inputText) {
        String normalizedInput = StringUtils.hasText(inputText) ? inputText.trim() : DEFAULT_TEST_TEXT;
        TestResponse response = runTest(normalizedInput);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", response.success());
        payload.put("text", response.text());
        payload.put("source", response.metadata().getGenerationSource().name());
        payload.put("sourceLabel", response.sourceLabel());
        payload.put("modelName", response.metadata().getModelName());
        payload.put("timeoutOccurred", response.metadata().isTimeoutOccurred());
        payload.put("fallbackReason", response.metadata().getFallbackReason());
        payload.put("primaryFailureReason", response.metadata().getPrimaryFailureReason());
        payload.put("secondaryFailureReason", response.metadata().getSecondaryFailureReason());
        payload.put("primaryFailureLayer", response.metadata().getPrimaryFailureLayer().name());
        payload.put("primaryFailureType", response.metadata().getPrimaryFailureType().name());
        payload.put("secondaryFailureLayer", response.metadata().getSecondaryFailureLayer().name());
        payload.put("secondaryFailureType", response.metadata().getSecondaryFailureType().name());
        return payload;
    }

    private TestResponse runTest(String normalizedInput) {
        log.info("Received AI test request: route=/admin/ai-test, inputLength={}", normalizedInput.length());
        try {
            AiTextGenerationResult result = aiFacadeService.testConnectivityMinimalPlainText(normalizedInput);
            AiGenerationMetadata metadata = result != null && result.getMetadata() != null
                    ? result.getMetadata()
                    : AiGenerationMetadata.fallback(
                    "local-rule",
                    "AI test returned empty metadata.",
                    "No metadata returned by service.",
                    "",
                    false,
                    AiFailureLayer.UNKNOWN,
                    AiFailureType.UNKNOWN_EXCEPTION,
                    AiFailureLayer.NONE,
                    AiFailureType.NONE
            );

            boolean success = metadata.getGenerationSource() != GenerationSource.FALLBACK;
            if (StringUtils.hasText(metadata.getPrimaryFailureReason())) {
                log.warn("AI test primary failure: layer={}, type={}, reason={}",
                        metadata.getPrimaryFailureLayer(),
                        metadata.getPrimaryFailureType(),
                        metadata.getPrimaryFailureReason());
            }
            if (StringUtils.hasText(metadata.getSecondaryFailureReason())) {
                log.warn("AI test secondary failure: layer={}, type={}, reason={}",
                        metadata.getSecondaryFailureLayer(),
                        metadata.getSecondaryFailureType(),
                        metadata.getSecondaryFailureReason());
            }
            if (!success) {
                log.warn("AI test used local fallback: reason={}", metadata.getFallbackReason());
            }
            return new TestResponse(
                    success,
                    result == null ? "" : result.getText(),
                    metadata,
                    sourceLabel(metadata.getGenerationSource())
            );
        } catch (Exception ex) {
            String reason = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.getClass().getSimpleName();
            log.error("AI test request failed unexpectedly: {}", reason, ex);
            AiGenerationMetadata metadata = AiGenerationMetadata.fallback(
                    "local-rule",
                    "AI test exception: " + reason,
                    reason,
                    "",
                    false,
                    AiFailureLayer.UNKNOWN,
                    AiFailureType.UNKNOWN_EXCEPTION,
                    AiFailureLayer.NONE,
                    AiFailureType.NONE
            );
            return new TestResponse(
                    false,
                    "Rule fallback: AI service is unavailable. Please check key and network.",
                    metadata,
                    sourceLabel(metadata.getGenerationSource())
            );
        }
    }

    private void fillBaseModel(Model model) {
        model.addAttribute("geminiEnabled", aiFacadeService.isGeminiEnabled());
        model.addAttribute("geminiModelName", aiFacadeService.getConfiguredModelName());
        model.addAttribute("geminiApiKeySource", aiFacadeService.getApiKeySource());
        model.addAttribute("geminiTimeoutSeconds", aiFacadeService.getGeminiTimeoutSeconds());

        model.addAttribute("qwenEnabled", aiFacadeService.isQwenEnabled());
        model.addAttribute("qwenModelName", aiFacadeService.getQwenConfiguredModelName());
        model.addAttribute("qwenApiKeySource", aiFacadeService.getQwenApiKeySource());
        model.addAttribute("qwenTimeoutSeconds", aiFacadeService.getQwenTimeoutSeconds());
    }

    private String sourceLabel(GenerationSource source) {
        if (source == GenerationSource.GEMINI) {
            return "Gemini";
        }
        if (source == GenerationSource.QWEN) {
            return "Qwen";
        }
        return "Fallback";
    }

    private record TestResponse(boolean success,
                                String text,
                                AiGenerationMetadata metadata,
                                String sourceLabel) {
    }
}
