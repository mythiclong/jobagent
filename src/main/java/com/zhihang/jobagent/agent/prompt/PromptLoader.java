package com.zhihang.jobagent.agent.prompt;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Loads versioned Agent prompts from the classpath.
 *
 * <p>The loader intentionally exposes named prompt keys rather than accepting
 * arbitrary paths. This keeps prompt access reviewable and prevents a caller
 * from reading files outside the prompt directory.</p>
 */
@Component
public class PromptLoader {

    public static final String SYSTEM = "system.md";
    public static final String RESUME_ANALYSIS = "resume-analysis.md";
    public static final String OUTPUT_SCHEMA = "output-schema.md";

    private static final Map<String, String> PROMPT_PATHS = Map.of(
            SYSTEM, "prompts/system.md",
            RESUME_ANALYSIS, "prompts/resume-analysis.md",
            OUTPUT_SCHEMA, "prompts/output-schema.md"
    );

    private final ResourceLoader resourceLoader;

    public PromptLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String load(String promptName) {
        String path = PROMPT_PATHS.get(promptName);
        if (path == null) {
            throw new IllegalArgumentException("Unknown Agent prompt: " + promptName);
        }

        Resource resource = resourceLoader.getResource("classpath:" + path);
        if (!resource.exists()) {
            throw new IllegalStateException("Agent prompt resource is missing: " + path);
        }

        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }
            String prompt = content.toString().trim();
            if (!StringUtils.hasText(prompt)) {
                throw new IllegalStateException("Agent prompt resource is empty: " + path);
            }
            return prompt;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to read Agent prompt: " + path, ex);
        }
    }

    public String version(String promptName) {
        String prompt = load(promptName);
        String marker = "<!-- prompt-version:";
        int markerStart = prompt.indexOf(marker);
        if (markerStart < 0) {
            throw new IllegalStateException("Agent prompt has no version marker: " + promptName);
        }
        int valueStart = markerStart + marker.length();
        int valueEnd = prompt.indexOf("-->", valueStart);
        if (valueEnd < 0) {
            throw new IllegalStateException("Agent prompt has an invalid version marker: " + promptName);
        }
        String version = prompt.substring(valueStart, valueEnd).trim();
        if (!StringUtils.hasText(version)) {
            throw new IllegalStateException("Agent prompt has an empty version: " + promptName);
        }
        return version;
    }
}
