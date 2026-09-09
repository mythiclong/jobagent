package com.zhihang.jobagent.service;

import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.zhihang.jobagent.dto.AiFailureLayer;
import com.zhihang.jobagent.dto.AiFailureType;
import com.zhihang.jobagent.dto.GenerationSource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class GeminiAiProvider implements AiProvider {

    private final GeminiClientService geminiClientService;

    public GeminiAiProvider(GeminiClientService geminiClientService) {
        this.geminiClientService = geminiClientService;
    }

    @Override
    public GenerationSource source() {
        return GenerationSource.GEMINI;
    }

    @Override
    public boolean isEnabled() {
        return geminiClientService.isGeminiEnabled();
    }

    @Override
    public String getConfiguredModelName() {
        return geminiClientService.getModelName();
    }

    @Override
    public String getApiKeySource() {
        return geminiClientService.getApiKeySource();
    }

    @Override
    public int getTimeoutSeconds() {
        return geminiClientService.getTimeoutSeconds();
    }

    @Override
    public AiProviderResult generate(String action, String prompt, float temperature) {
        Optional<Client> clientOptional = geminiClientService.createClientIfConfigured();
        if (clientOptional.isEmpty()) {
            return AiProviderResult.failure(
                    source(),
                    getConfiguredModelName(),
                    geminiClientService.buildMissingKeyReason(),
                    false,
                    AiFailureLayer.CONFIG,
                    AiFailureType.API_KEY_MISSING
            );
        }

        Client client = clientOptional.get();
        GenerateContentConfig config = GenerateContentConfig.builder().temperature(temperature).build();

        try {
            CompletableFuture<GenerateContentResponse> future = CompletableFuture.supplyAsync(() -> {
                try (Client closable = client) {
                    return closable.models.generateContent(getConfiguredModelName(), prompt, config);
                }
            });

            GenerateContentResponse response = future.get(getTimeoutSeconds(), TimeUnit.SECONDS);
            String modelName = response.modelVersion()
                    .filter(StringUtils::hasText)
                    .orElse(getConfiguredModelName());
            String text = defaultText(response.text());
            if (!StringUtils.hasText(text)) {
                return AiProviderResult.failure(
                        source(),
                        modelName,
                        "Gemini returned empty content.",
                        false,
                        AiFailureLayer.MODEL,
                        AiFailureType.RESPONSE_EMPTY
                );
            }
            return AiProviderResult.success(source(), text, modelName);
        } catch (TimeoutException ex) {
            return AiProviderResult.failure(
                    source(),
                    getConfiguredModelName(),
                    "Gemini request timeout after " + getTimeoutSeconds() + "s.",
                    true,
                    AiFailureLayer.NETWORK,
                    AiFailureType.CONNECTION_TIMEOUT
            );
        } catch (ApiException ex) {
            String reason = "Gemini API error: HTTP " + ex.code() + " " + ex.status() + " | " + defaultText(ex.message());
            AiFailureType type = (ex.code() == 401 || ex.code() == 403) ? AiFailureType.AUTH_401_403 : AiFailureType.HTTP_NON_SUCCESS;
            return AiProviderResult.failure(
                    source(),
                    getConfiguredModelName(),
                    reason,
                    false,
                    AiFailureLayer.MODEL,
                    type
            );
        } catch (Exception ex) {
            FailureDiagnosis diagnosis = diagnoseFailure(ex);
            String reason = "Gemini request failed: " + diagnosis.reason();
            return AiProviderResult.failure(
                    source(),
                    getConfiguredModelName(),
                    reason,
                    diagnosis.timeoutOccurred(),
                    diagnosis.layer(),
                    diagnosis.type()
            );
        }
    }

    private FailureDiagnosis diagnoseFailure(Throwable throwable) {
        Throwable root = unwrap(throwable);
        if (root instanceof TimeoutException
                || root instanceof HttpTimeoutException
                || root instanceof SocketTimeoutException) {
            return new FailureDiagnosis(
                    "connection timeout",
                    true,
                    AiFailureLayer.NETWORK,
                    AiFailureType.CONNECTION_TIMEOUT
            );
        }
        if (root instanceof UnknownHostException) {
            return new FailureDiagnosis(
                    "DNS resolution failed: " + defaultText(root.getMessage()),
                    false,
                    AiFailureLayer.NETWORK,
                    AiFailureType.DNS_FAILURE
            );
        }
        if (root instanceof SSLHandshakeException || root instanceof SSLException) {
            return new FailureDiagnosis(
                    "TLS/SSL handshake failed: " + defaultText(root.getMessage()),
                    false,
                    AiFailureLayer.NETWORK,
                    AiFailureType.TLS_HANDSHAKE_FAILURE
            );
        }
        if (root instanceof ConnectException) {
            return new FailureDiagnosis(
                    "network connect failed: " + defaultText(root.getMessage()),
                    false,
                    AiFailureLayer.NETWORK,
                    AiFailureType.UNKNOWN_EXCEPTION
            );
        }
        return new FailureDiagnosis(
                defaultText(root.getMessage()),
                false,
                AiFailureLayer.UNKNOWN,
                AiFailureType.UNKNOWN_EXCEPTION
        );
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof ExecutionException && current.getCause() != null) {
            current = current.getCause();
        }
        while (current.getCause() != null
                && !(current instanceof UnknownHostException)
                && !(current instanceof SSLException)
                && !(current instanceof ConnectException)
                && !(current instanceof SocketTimeoutException)
                && !(current instanceof TimeoutException)
                && !(current instanceof HttpTimeoutException)) {
            current = current.getCause();
        }
        return current == null ? throwable : current;
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
    }

    private record FailureDiagnosis(String reason,
                                    boolean timeoutOccurred,
                                    AiFailureLayer layer,
                                    AiFailureType type) {
    }
}
