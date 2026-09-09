package com.zhihang.jobagent.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class QwenClientService {

    private static final Logger log = LoggerFactory.getLogger(QwenClientService.class);

    public static final String FIXED_ENDPOINT = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";
    public static final String FIXED_MODEL_NAME = "qwen3.5-plus";
    public static final int FIXED_CONNECT_TIMEOUT_SECONDS = 30;
    public static final int FIXED_REQUEST_TIMEOUT_SECONDS = 60;

    private static final String KEY_SOURCE_ENV_DASHSCOPE = "ENV:DASHSCOPE_API_KEY";
    private static final String KEY_SOURCE_ENV_QWEN = "ENV:QWEN_API_KEY";
    private static final String KEY_SOURCE_PROPERTY = "PROPERTY:jobagent.ai.qwen.api-key";
    private static final String KEY_SOURCE_NONE = "NONE";

    @Value("${jobagent.ai.qwen.enabled:true}")
    private boolean enabled;

    @Value("${jobagent.ai.qwen.model:qwen3.5-plus}")
    private String configuredModelName;

    @Value("${jobagent.ai.qwen.timeout-seconds:60}")
    private int configuredTimeoutSeconds;

    @Value("${jobagent.ai.qwen.endpoint:https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions}")
    private String configuredEndpoint;

    @Value("${jobagent.ai.qwen.api-key:}")
    private String legacyPropertyApiKey;

    private volatile String lastLoggedKeySource = "";

    public boolean isEnabledByConfig() {
        return enabled;
    }

    public boolean isQwenEnabled() {
        if (!enabled) {
            return false;
        }
        return resolveApiKey().apiKey().isPresent();
    }

    public String getApiKeySource() {
        if (!enabled) {
            return "DISABLED";
        }
        return resolveApiKey().keySource();
    }

    public String getModelName() {
        return FIXED_MODEL_NAME;
    }

    public String getEndpoint() {
        return FIXED_ENDPOINT;
    }

    public int getConnectTimeoutSeconds() {
        return FIXED_CONNECT_TIMEOUT_SECONDS;
    }

    public int getRequestTimeoutSeconds() {
        return FIXED_REQUEST_TIMEOUT_SECONDS;
    }

    public String getConfiguredModelNameValue() {
        return defaultText(configuredModelName);
    }

    public String getConfiguredEndpointValue() {
        return defaultText(configuredEndpoint);
    }

    public int getConfiguredTimeoutSecondsValue() {
        return Math.max(configuredTimeoutSeconds, 1);
    }

    public int getTimeoutSeconds() {
        return getRequestTimeoutSeconds();
    }

    public Optional<String> resolveApiKeyValue() {
        if (!enabled) {
            logDisabledOnce();
            return Optional.empty();
        }
        ApiKeyResolution resolution = resolveApiKey();
        logKeySource(resolution.keySource());
        return resolution.apiKey();
    }

    public String buildMissingKeyReason() {
        if (!enabled) {
            return "Qwen provider disabled by config (jobagent.ai.qwen.enabled=false).";
        }
        return "Qwen API key is missing. Configure DASHSCOPE_API_KEY (or QWEN_API_KEY).";
    }

    private ApiKeyResolution resolveApiKey() {
        String dashscopeKey = defaultText(System.getenv("DASHSCOPE_API_KEY"));
        if (StringUtils.hasText(dashscopeKey)) {
            return new ApiKeyResolution(Optional.of(dashscopeKey), KEY_SOURCE_ENV_DASHSCOPE);
        }

        String qwenKey = defaultText(System.getenv("QWEN_API_KEY"));
        if (StringUtils.hasText(qwenKey)) {
            return new ApiKeyResolution(Optional.of(qwenKey), KEY_SOURCE_ENV_QWEN);
        }

        String propertyKey = defaultText(legacyPropertyApiKey);
        if (StringUtils.hasText(propertyKey)) {
            return new ApiKeyResolution(Optional.of(propertyKey), KEY_SOURCE_PROPERTY);
        }

        return new ApiKeyResolution(Optional.empty(), KEY_SOURCE_NONE);
    }

    private void logDisabledOnce() {
        String state = "DISABLED";
        if (state.equals(lastLoggedKeySource)) {
            return;
        }
        lastLoggedKeySource = state;
        log.warn("Qwen provider is disabled by configuration.");
    }

    private void logKeySource(String keySource) {
        if (keySource.equals(lastLoggedKeySource)) {
            return;
        }
        lastLoggedKeySource = keySource;

        if (KEY_SOURCE_ENV_DASHSCOPE.equals(keySource) || KEY_SOURCE_ENV_QWEN.equals(keySource)) {
            log.info("Qwen key source: {} (recommended env)", keySource);
            return;
        }
        if (KEY_SOURCE_PROPERTY.equals(keySource)) {
            log.warn("Qwen key source: {} (legacy fallback). Recommend DASHSCOPE_API_KEY env var.", keySource);
            return;
        }
        log.warn("Qwen key source: {}. Calls may fallback.", keySource);
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }

    private record ApiKeyResolution(Optional<String> apiKey, String keySource) {
    }
}
