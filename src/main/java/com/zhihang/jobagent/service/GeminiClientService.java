package com.zhihang.jobagent.service;

import com.google.genai.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
public class GeminiClientService {

    private static final Logger log = LoggerFactory.getLogger(GeminiClientService.class);

    private static final String KEY_SOURCE_ENV = "ENV:GEMINI_API_KEY";
    private static final String KEY_SOURCE_PROPERTY = "PROPERTY:jobagent.ai.gemini.api-key";
    private static final String KEY_SOURCE_NONE = "NONE";

    @Value("${jobagent.ai.gemini.enabled:true}")
    private boolean enabled;

    @Value("${jobagent.ai.gemini.model:gemini-2.5-flash}")
    private String modelName;

    @Value("${jobagent.ai.gemini.timeout-seconds:15}")
    private int timeoutSeconds;

    @Value("${jobagent.ai.gemini.api-key:}")
    private String legacyPropertyApiKey;

    private volatile String lastLoggedKeySource = "";

    public boolean isEnabledByConfig() {
        return enabled;
    }

    public String getModelName() {
        return modelName;
    }

    public int getTimeoutSeconds() {
        return Math.max(timeoutSeconds, 1);
    }

    public boolean isGeminiEnabled() {
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

    public Optional<Client> createClientIfConfigured() {
        if (!enabled) {
            logDisabledOnce();
            return Optional.empty();
        }

        ApiKeyResolution apiKeyResolution = resolveApiKey();
        logKeySource(apiKeyResolution.keySource());
        if (apiKeyResolution.apiKey().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(Client.builder().apiKey(apiKeyResolution.apiKey().get()).build());
    }

    public String buildMissingKeyReason() {
        if (!enabled) {
            return "Gemini provider disabled by config (jobagent.ai.gemini.enabled=false).";
        }
        return "Gemini API key is missing. Configure GEMINI_API_KEY environment variable first.";
    }

    private ApiKeyResolution resolveApiKey() {
        String envKey = defaultText(System.getenv("GEMINI_API_KEY"));
        if (StringUtils.hasText(envKey)) {
            return new ApiKeyResolution(Optional.of(envKey), KEY_SOURCE_ENV);
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
        log.warn("Gemini provider is disabled by configuration.");
    }

    private void logKeySource(String keySource) {
        if (keySource.equals(lastLoggedKeySource)) {
            return;
        }
        lastLoggedKeySource = keySource;

        if (KEY_SOURCE_ENV.equals(keySource)) {
            log.info("Gemini key source: {} (recommended)", keySource);
            return;
        }
        if (KEY_SOURCE_PROPERTY.equals(keySource)) {
            log.warn("Gemini key source: {} (legacy fallback). Recommend GEMINI_API_KEY environment variable.", keySource);
            return;
        }
        log.warn("Gemini key source: {}. AI calls may fallback.", keySource);
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }

    private record ApiKeyResolution(Optional<String> apiKey, String keySource) {
    }
}
