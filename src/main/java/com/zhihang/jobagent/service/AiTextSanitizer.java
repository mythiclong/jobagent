package com.zhihang.jobagent.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class AiTextSanitizer {

    public String sanitizePlainText(String text) {
        String normalized = normalizeBasic(text);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        String[] lines = normalized.split("\n");
        boolean previousBlank = false;
        for (String rawLine : lines) {
            String line = sanitizeLine(rawLine);
            if (!StringUtils.hasText(line)) {
                if (!previousBlank && !sb.isEmpty()) {
                    sb.append('\n');
                }
                previousBlank = true;
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line);
            previousBlank = false;
        }
        return sb.toString().trim();
    }

    public List<String> sanitizeListItems(List<String> rawItems, int maxItems) {
        Set<String> items = new LinkedHashSet<>();
        if (rawItems == null || rawItems.isEmpty()) {
            return List.of();
        }

        for (String rawItem : rawItems) {
            String cleaned = sanitizePlainText(rawItem);
            if (!StringUtils.hasText(cleaned)) {
                continue;
            }

            for (String line : cleaned.split("\n")) {
                String item = stripListPrefix(line);
                if (!StringUtils.hasText(item)) {
                    continue;
                }
                items.add(item);
                if (items.size() >= maxItems) {
                    return new ArrayList<>(items);
                }
            }
        }
        return new ArrayList<>(items);
    }

    public String sanitizeResumeText(String text) {
        String normalized = normalizeBasic(text);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }

        String[] lines = normalized.split("\n");
        StringBuilder sb = new StringBuilder();
        boolean previousBlank = false;
        for (String rawLine : lines) {
            String line = sanitizeLine(rawLine);
            if (!StringUtils.hasText(line)) {
                if (!previousBlank && !sb.isEmpty()) {
                    sb.append('\n');
                }
                previousBlank = true;
                continue;
            }

            String cleaned = normalizeResumeLine(line);
            if (!StringUtils.hasText(cleaned)) {
                continue;
            }

            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(cleaned);
            previousBlank = false;
        }
        return sb.toString().trim();
    }

    private String normalizeBasic(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\u0000", "")
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("```", "")
                .replace("`", "")
                .replace("**", "")
                .replace("__", "")
                .replace('\u00A0', ' ')
                .trim();
    }

    private String sanitizeLine(String rawLine) {
        String line = rawLine == null ? "" : rawLine.trim();
        if (!StringUtils.hasText(line)) {
            return "";
        }
        line = line.replaceAll("^#{1,6}\\s*", "");
        line = line.replaceAll("^>\\s*", "");
        line = line.replaceAll("^\\*{1,3}\\s*", "");
        line = line.replaceAll("\\s{2,}", " ");
        return line.trim();
    }

    private String normalizeResumeLine(String line) {
        String cleaned = line.trim();
        if (!StringUtils.hasText(cleaned)) {
            return "";
        }

        if (isBulletLine(cleaned)) {
            return "- " + stripListPrefix(cleaned);
        }

        String heading = stripHeadingSuffix(cleaned);
        if (isResumeHeading(heading)) {
            return heading + "：";
        }
        return cleaned;
    }

    private boolean isBulletLine(String line) {
        return line.matches("^([-*•]|\\d+[\\.、\\)]|[一二三四五六七八九十]+[、\\)])\\s+.*");
    }

    private String stripListPrefix(String line) {
        return line.replaceFirst("^([-*•]|\\d+[\\.、\\)]|[一二三四五六七八九十]+[、\\)])\\s*", "").trim();
    }

    private String stripHeadingSuffix(String line) {
        return line.replaceAll("[：:]+$", "").trim();
    }

    private boolean isResumeHeading(String line) {
        String normalized = line.replaceAll("\\s+", "");
        return normalized.equals("基本信息")
                || normalized.equals("个人信息")
                || normalized.equals("教育经历")
                || normalized.equals("教育背景")
                || normalized.equals("技能描述")
                || normalized.equals("专业技能")
                || normalized.equals("项目经历")
                || normalized.equals("项目经验")
                || normalized.equals("实习经历")
                || normalized.equals("校园经历")
                || normalized.equals("工作经历")
                || normalized.equals("获奖情况")
                || normalized.equals("证书")
                || normalized.equals("自我评价")
                || normalized.equals("求职意向")
                || normalized.equals("其他");
    }
}
