package com.zhihang.jobagent.service.update;

import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;

public final class UpdateSourceSupport {

    private UpdateSourceSupport() {
    }

    public static String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        return normalized.replace('\u00a0', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String normalizeUrl(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        try {
            URI uri = new URI(value.trim());
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "/" : uri.getPath();
            String query = uri.getQuery();
            URI normalized = new URI(
                    scheme,
                    uri.getUserInfo(),
                    host,
                    port,
                    path,
                    query,
                    null
            );
            String asString = normalized.toASCIIString();
            return asString.endsWith("/") && path.length() > 1
                    ? asString.substring(0, asString.length() - 1)
                    : asString;
        } catch (URISyntaxException ex) {
            return value.trim();
        }
    }

    public static String sha256(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(defaultString(source).getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not supported", ex);
        }
    }

    public static String defaultString(String value) {
        return value == null ? "" : value.trim();
    }

    public static String truncate(String value, int maxLength) {
        String normalized = defaultString(value);
        if (maxLength <= 0 || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }
}
