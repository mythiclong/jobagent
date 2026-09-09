package com.zhihang.jobagent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhihang.jobagent.dto.AiFailureLayer;
import com.zhihang.jobagent.dto.AiFailureType;
import com.zhihang.jobagent.dto.GenerationSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Service
public class QwenAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(QwenAiProvider.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final HttpClient.Version HTTP_VERSION = HttpClient.Version.HTTP_1_1;
    private static final ProxySelector NO_PROXY_SELECTOR = new NoProxySelector();
    private static final String PROXY_MODE_DIRECT = "no explicit proxy (NoProxySelector)";

    private final QwenClientService qwenClientService;

    public QwenAiProvider(QwenClientService qwenClientService) {
        this.qwenClientService = qwenClientService;
    }

    @Override
    public GenerationSource source() {
        return GenerationSource.QWEN;
    }

    @Override
    public boolean isEnabled() {
        return qwenClientService.isQwenEnabled();
    }

    @Override
    public String getConfiguredModelName() {
        return qwenClientService.getModelName();
    }

    @Override
    public String getApiKeySource() {
        return qwenClientService.getApiKeySource();
    }

    @Override
    public int getTimeoutSeconds() {
        return qwenClientService.getTimeoutSeconds();
    }

    @Override
    public AiProviderResult generate(String action, String prompt, float ignoredTemperature) {
        Optional<String> apiKeyOptional = qwenClientService.resolveApiKeyValue();
        if (apiKeyOptional.isEmpty()) {
            return AiProviderResult.failure(
                    source(),
                    getConfiguredModelName(),
                    qwenClientService.buildMissingKeyReason(),
                    false,
                    AiFailureLayer.CONFIG,
                    AiFailureType.API_KEY_MISSING
            );
        }

        String endpoint = qwenClientService.getEndpoint();
        int connectTimeoutSeconds = qwenClientService.getConnectTimeoutSeconds();
        int requestTimeoutSeconds = qwenClientService.getRequestTimeoutSeconds();
        String step = "build_client";
        HttpClient httpClient;
        try {
            httpClient = HttpClient.newBuilder()
                    .version(HTTP_VERSION)
                    .proxy(NO_PROXY_SELECTOR)
                    .connectTimeout(Duration.ofSeconds(connectTimeoutSeconds))
                    .build();
        } catch (Exception ex) {
            return failureForException(step, ex, requestTimeoutSeconds);
        }

        HttpRequest request;
        String body;
        URI endpointUri;
        try {
            step = "build_request";
            endpointUri = URI.create(endpoint);
            body = buildRequestBody(prompt);
            request = HttpRequest.newBuilder(endpointUri)
                    .timeout(Duration.ofSeconds(requestTimeoutSeconds))
                    .version(HTTP_VERSION)
                    .header("Authorization", "Bearer " + apiKeyOptional.get())
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            logRequestContext(action, endpointUri, connectTimeoutSeconds, requestTimeoutSeconds, body);
        } catch (Exception ex) {
            return failureForException(step, ex, requestTimeoutSeconds);
        }

        HttpResponse<String> response;
        try {
            step = "send";
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return failureForException(step, ex, requestTimeoutSeconds);
        }

        try {
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String reason = "Qwen API error at step=send: HTTP " + response.statusCode()
                        + " | " + summarizeBody(response.body());
                AiFailureType type = (response.statusCode() == 401 || response.statusCode() == 403)
                        ? AiFailureType.AUTH_401_403
                        : AiFailureType.HTTP_NON_SUCCESS;
                log.warn("Qwen request failed: step=send, statusCode={}, responseBodySummary={}",
                        response.statusCode(), summarizeBody(response.body()));
                return AiProviderResult.failure(
                        source(),
                        getConfiguredModelName(),
                        reason,
                        false,
                        AiFailureLayer.MODEL,
                        type
                );
            }

            step = "parse";
            String content;
            try {
                content = parseContent(response.body());
            } catch (Exception parseEx) {
                Throwable root = unwrap(parseEx);
                log.error("Qwen request failed: step=parse, exceptionClass={}, message={}",
                        root.getClass().getName(), defaultText(root.getMessage()), parseEx);
                return AiProviderResult.failure(
                        source(),
                        getConfiguredModelName(),
                        "Qwen response parse failed at step=parse: "
                                + root.getClass().getName()
                                + " | " + defaultText(root.getMessage()),
                        false,
                        AiFailureLayer.MODEL,
                        AiFailureType.RESPONSE_PARSE_FAILED
                );
            }
            if (!StringUtils.hasText(content)) {
                return AiProviderResult.failure(
                        source(),
                        getConfiguredModelName(),
                        "Qwen returned empty content.",
                        false,
                        AiFailureLayer.MODEL,
                        AiFailureType.RESPONSE_EMPTY
                );
            }
            String modelName = parseModelName(response.body());
            return AiProviderResult.success(source(), content, modelName);
        } catch (Exception ex) {
            return failureForException(step, ex, requestTimeoutSeconds);
        }
    }

    private String buildRequestBody(String prompt) throws Exception {
        ObjectNode root = OBJECT_MAPPER.createObjectNode();
        root.put("model", getConfiguredModelName());
        ArrayNode messages = root.putArray("messages");
        messages.addObject().put("role", "user").put("content", defaultText(prompt));
        root.put("enable_thinking", false);
        return OBJECT_MAPPER.writeValueAsString(root);
    }

    private String parseContent(String responseBody) throws Exception {
        JsonNode root = OBJECT_MAPPER.readTree(defaultText(responseBody));
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "";
        }
        JsonNode contentNode = choices.get(0).path("message").path("content");
        if (contentNode.isTextual()) {
            return contentNode.asText("").trim();
        }
        return contentNode.toString().trim();
    }

    private String parseModelName(String responseBody) {
        try {
            JsonNode root = OBJECT_MAPPER.readTree(defaultText(responseBody));
            String model = root.path("model").asText("");
            return StringUtils.hasText(model) ? model : getConfiguredModelName();
        } catch (Exception ex) {
            return getConfiguredModelName();
        }
    }

    private void logRequestContext(String action,
                                   URI endpointUri,
                                   int connectTimeoutSeconds,
                                   int requestTimeoutSeconds,
                                   String body) {
        String configuredEndpoint = qwenClientService.getConfiguredEndpointValue();
        String configuredModel = qwenClientService.getConfiguredModelNameValue();
        int configuredTimeout = qwenClientService.getConfiguredTimeoutSecondsValue();
        String baseUrl = extractBaseUrl(endpointUri);

        log.info(
                "Qwen request config: action={}, baseUrl={}, finalEndpoint={}, modelName={}, connectTimeout={}s, requestTimeout={}s, httpVersion={}, proxy={}, requestBodySummary={}",
                defaultText(action),
                baseUrl,
                endpointUri.toString(),
                getConfiguredModelName(),
                connectTimeoutSeconds,
                requestTimeoutSeconds,
                HTTP_VERSION,
                PROXY_MODE_DIRECT,
                summarizeBody(body)
        );
        log.info("Qwen transport diagnostics: finalEndpoint={}, httpVersion={}, proxyEnabled=false, proxyMode={}, trustStorePath={}",
                endpointUri.toString(), HTTP_VERSION, PROXY_MODE_DIRECT, resolveTrustStorePath());

        if (StringUtils.hasText(configuredEndpoint) && !configuredEndpoint.equals(endpointUri.toString())) {
            log.warn("Qwen endpoint override ignored: configuredEndpoint={}, fixedEndpoint={}",
                    configuredEndpoint, endpointUri);
        }
        if (StringUtils.hasText(configuredModel) && !configuredModel.equals(getConfiguredModelName())) {
            log.warn("Qwen model override ignored: configuredModel={}, fixedModel={}",
                    configuredModel, getConfiguredModelName());
        }
        if (configuredTimeout != requestTimeoutSeconds) {
            log.warn("Qwen timeout override ignored: configuredTimeout={}s, fixedRequestTimeout={}s, fixedConnectTimeout={}s",
                    configuredTimeout, requestTimeoutSeconds, connectTimeoutSeconds);
        }
    }

    private AiProviderResult failureForException(String step, Exception ex, int requestTimeoutSeconds) {
        FailureDiagnosis diagnosis = diagnoseFailure(ex);
        Throwable root = unwrap(ex);
        String exceptionClass = root.getClass().getName();
        String message = defaultText(root.getMessage());
        log.error("Qwen request failed: step={}, exceptionClass={}, message={}, trustStorePath={}, proxyEnabled=false, proxyMode={}",
                step, exceptionClass, message, resolveTrustStorePath(), PROXY_MODE_DIRECT, ex);
        if (diagnosis.type() == AiFailureType.TLS_HANDSHAKE_FAILURE) {
            log.error("Qwen TLS handshake diagnostics: detail={}, trustStorePath={}, proxyEnabled=false, proxyMode={}",
                    diagnosis.reason(), resolveTrustStorePath(), PROXY_MODE_DIRECT);
        }
        String reason;
        if (diagnosis.type() == AiFailureType.CONNECTION_TIMEOUT) {
            reason = "Qwen request timeout at step=" + step
                    + " after requestTimeout=" + requestTimeoutSeconds + "s"
                    + ", exceptionClass=" + exceptionClass
                    + ", message=" + message;
        } else {
            reason = "Qwen request failed at step=" + step
                    + ", exceptionClass=" + exceptionClass
                    + ", message=" + message
                    + ", detail=" + diagnosis.reason();
        }
        return AiProviderResult.failure(
                source(),
                getConfiguredModelName(),
                reason,
                diagnosis.timeoutOccurred(),
                diagnosis.layer(),
                diagnosis.type()
        );
    }

    private String extractBaseUrl(URI uri) {
        if (uri == null || !StringUtils.hasText(uri.getHost())) {
            return "";
        }
        String scheme = StringUtils.hasText(uri.getScheme()) ? uri.getScheme() : "https";
        int port = uri.getPort();
        if (port > 0) {
            return scheme + "://" + uri.getHost() + ":" + port;
        }
        return scheme + "://" + uri.getHost();
    }

    private String summarizeBody(String body) {
        String text = defaultText(body).replace("\n", "\\n");
        if (text.length() <= 320) {
            return text;
        }
        return text.substring(0, 320) + "...";
    }

    private String defaultText(String text) {
        return text == null ? "" : text.trim();
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
            String tlsDetail = buildTlsFailureDetail(root);
            if (isCertificateOrTrustStoreIssue(root)) {
                return new FailureDiagnosis(
                        "TLS handshake failure [certificate/truststore issue]: " + tlsDetail,
                        false,
                        AiFailureLayer.NETWORK,
                        AiFailureType.TLS_HANDSHAKE_FAILURE
                );
            }
            if (isProxyIssue(root)) {
                return new FailureDiagnosis(
                        "TLS handshake failure [proxy issue suspected]: " + tlsDetail,
                        false,
                        AiFailureLayer.NETWORK,
                        AiFailureType.TLS_HANDSHAKE_FAILURE
                );
            }
            return new FailureDiagnosis(
                    "TLS handshake failure [generic]: " + tlsDetail,
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
                root.getClass().getName() + ": " + defaultText(root.getMessage()),
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

    private boolean isCertificateOrTrustStoreIssue(Throwable throwable) {
        String text = collectThrowableText(throwable).toLowerCase(Locale.ROOT);
        return text.contains("pkix")
                || text.contains("unable to find valid certification path")
                || text.contains("certificate")
                || text.contains("truststore")
                || text.contains("trust manager")
                || text.contains("sun.security.validator");
    }

    private boolean isProxyIssue(Throwable throwable) {
        String text = collectThrowableText(throwable).toLowerCase(Locale.ROOT);
        return text.contains("proxy")
                || text.contains("407")
                || text.contains("tunnel")
                || text.contains("http connect")
                || text.contains("mitm");
    }

    private String buildTlsFailureDetail(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 5) {
            if (depth > 0) {
                builder.append(" <- ");
            }
            builder.append(current.getClass().getSimpleName())
                    .append(":")
                    .append(defaultText(current.getMessage()));
            current = current.getCause();
            depth++;
        }
        return builder.toString();
    }

    private String collectThrowableText(Throwable throwable) {
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 8) {
            builder.append(' ')
                    .append(current.getClass().getName())
                    .append(' ')
                    .append(defaultText(current.getMessage()));
            current = current.getCause();
            depth++;
        }
        return builder.toString().trim();
    }

    private String resolveTrustStorePath() {
        String configuredPath = defaultText(System.getProperty("javax.net.ssl.trustStore"));
        if (StringUtils.hasText(configuredPath)) {
            return configuredPath;
        }
        String javaHome = defaultText(System.getProperty("java.home"));
        if (StringUtils.hasText(javaHome)) {
            return javaHome + File.separator + "lib" + File.separator + "security" + File.separator + "cacerts (JVM default)";
        }
        return "JVM default trust store";
    }

    private static final class NoProxySelector extends ProxySelector {
        @Override
        public List<Proxy> select(URI uri) {
            return List.of(Proxy.NO_PROXY);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            log.warn("Qwen no-proxy selector connectFailed: uri={}, socketAddress={}, message={}",
                    uri, sa, ioe == null ? "" : ioe.getMessage());
        }
    }

    private record FailureDiagnosis(String reason,
                                    boolean timeoutOccurred,
                                    AiFailureLayer layer,
                                    AiFailureType type) {
    }
}
