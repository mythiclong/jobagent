package com.zhihang.jobagent.support;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class JobDisplayFormatter {

    private static final String PLACEHOLDER = "-";
    private static final int DEFAULT_SUMMARY_PREVIEW_LIMIT = 132;
    private static final int DEFAULT_ADDRESS_PREVIEW_LIMIT = 18;

    private JobDisplayFormatter() {
    }

    public static String formatReadableLocation(String jobLocation, String companyAddress) {
        String normalizedLocation = normalizeInlineText(jobLocation);
        if (isReadableLocation(normalizedLocation)) {
            return normalizedLocation;
        }

        String addressSummary = extractReadableLocationFromAddress(companyAddress);
        if (isReadableLocation(addressSummary)) {
            return addressSummary;
        }

        return PLACEHOLDER;
    }

    public static String extractReadableLocationFromAddress(String companyAddress) {
        String normalizedAddress = normalizeInlineText(companyAddress);
        if (!containsChinese(normalizedAddress) || looksLikeLocationCode(normalizedAddress)) {
            return "";
        }

        String administrativeSummary = extractAdministrativeSummary(normalizedAddress);
        if (isReadableLocation(administrativeSummary)) {
            return administrativeSummary;
        }

        String firstSegment = takeFirstSegment(normalizedAddress);
        if (isReadableLocation(firstSegment)) {
            return shorten(firstSegment, DEFAULT_ADDRESS_PREVIEW_LIMIT);
        }

        return "";
    }

    public static String formatSalaryRange(BigDecimal salaryMin, BigDecimal salaryMax) {
        BigDecimal normalizedMin = JobSalaryNormalizer.normalizeStoredMonthlyComparableAmount(salaryMin);
        BigDecimal normalizedMax = JobSalaryNormalizer.normalizeStoredMonthlyComparableAmount(salaryMax);
        if (normalizedMin == null && normalizedMax == null) {
            return PLACEHOLDER;
        }

        if (normalizedMin != null && normalizedMax != null && normalizedMin.compareTo(normalizedMax) > 0) {
            BigDecimal swapped = normalizedMin;
            normalizedMin = normalizedMax;
            normalizedMax = swapped;
        }

        if (normalizedMin != null && normalizedMax != null) {
            if (normalizedMin.compareTo(normalizedMax) == 0) {
                return JobSalaryNormalizer.formatMonthlyDisplayAmount(normalizedMin) + "/月";
            }
            return JobSalaryNormalizer.formatMonthlyDisplayAmount(normalizedMin)
                    + "-"
                    + JobSalaryNormalizer.formatMonthlyDisplayAmount(normalizedMax)
                    + "/月";
        }

        return JobSalaryNormalizer.formatMonthlyDisplayAmount(normalizedMin != null ? normalizedMin : normalizedMax) + "/月";
    }

    public static String formatSalaryRange(BigDecimal salaryMin,
                                           BigDecimal salaryMax,
                                           String salaryDisplayText,
                                           String salaryUnitType) {
        String normalizedDisplayText = normalizeInlineText(salaryDisplayText);
        if (JobSalaryNormalizer.isMonthlyUnit(salaryUnitType)) {
            if (salaryMin != null || salaryMax != null) {
                return formatSalaryRange(salaryMin, salaryMax);
            }
            return StringUtils.hasText(normalizedDisplayText) ? normalizedDisplayText : PLACEHOLDER;
        }

        if (StringUtils.hasText(normalizedDisplayText)) {
            return normalizedDisplayText;
        }
        return PLACEHOLDER;
    }

    public static String resolvePreferredJobTitle(String displayTitle,
                                                  String jobName,
                                                  String jobTitle,
                                                  String rawTitle,
                                                  String companyName) {
        List<String> candidates = List.of(
                normalizeInlineText(displayTitle),
                normalizeInlineText(jobName),
                normalizeInlineText(jobTitle),
                normalizeInlineText(rawTitle)
        );

        for (String candidate : candidates) {
            if (isReadableJobTitle(candidate, companyName)) {
                return candidate;
            }
        }

        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }

        return PLACEHOLDER;
    }

    public static String createSummaryPreview(String summary) {
        return createSummaryPreview(summary, DEFAULT_SUMMARY_PREVIEW_LIMIT);
    }

    public static String createSummaryPreview(String summary, int limit) {
        String normalizedSummary = normalizeBlockText(summary);
        if (!StringUtils.hasText(normalizedSummary)) {
            return PLACEHOLDER;
        }
        if (normalizedSummary.length() <= limit) {
            return normalizedSummary;
        }
        return normalizedSummary.substring(0, limit).trim() + "...";
    }

    public static boolean isSummaryExpandable(String summary) {
        return isSummaryExpandable(summary, DEFAULT_SUMMARY_PREVIEW_LIMIT);
    }

    public static boolean isSummaryExpandable(String summary, int limit) {
        return normalizeBlockText(summary).length() > limit;
    }

    public static String normalizeDisplayText(String value) {
        String normalizedValue = normalizeInlineText(value);
        return StringUtils.hasText(normalizedValue) ? normalizedValue : PLACEHOLDER;
    }

    public static String resolveSourceTypeKey(String sourceName) {
        String normalizedSource = normalizeInlineText(sourceName).toUpperCase(Locale.ROOT);
        if ("SCHOOL_JOB_EXCEL".equals(normalizedSource)) {
            return "excel";
        }
        if ("MANUAL".equals(normalizedSource)) {
            return "manual";
        }
        return "other";
    }

    public static String resolveSourceTypeLabel(String sourceName) {
        return switch (resolveSourceTypeKey(sourceName)) {
            case "excel" -> "Excel 校招";
            case "manual" -> "手动维护";
            default -> "其他来源";
        };
    }

    public static String normalizeInlineText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replace('\u3000', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static String normalizeBlockText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replace('\u3000', ' ')
                .replaceAll("[\\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n+ *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private static boolean isReadableLocation(String value) {
        if (!StringUtils.hasText(value) || looksLikeLocationCode(value) || !containsChinese(value)) {
            return false;
        }

        String normalizedValue = normalizeInlineText(value);
        return normalizedValue.length() >= 2
                && !"全国".equals(normalizedValue)
                && !"不限".equals(normalizedValue)
                && !"待定".equals(normalizedValue)
                && !"若干".equals(normalizedValue);
    }

    private static boolean looksLikeLocationCode(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String normalizedValue = value.replaceAll("[\\s\\-_/]", "");
        return normalizedValue.matches("\\d{4,12}")
                || (!containsChinese(normalizedValue) && normalizedValue.matches("[A-Za-z]{0,4}\\d{4,12}"));
    }

    private static boolean isReadableJobTitle(String value, String companyName) {
        if (!StringUtils.hasText(value)) {
            return false;
        }

        String normalizedValue = normalizeInlineText(value);
        if (normalizedValue.length() < 2) {
            return false;
        }
        if (looksLikeLocationCode(normalizedValue) || normalizedValue.matches("[\\d\\-_/ ]+")) {
            return false;
        }
        if (containsOnlyGenericWords(normalizedValue)) {
            return false;
        }
        return !looksLikeCompanyEcho(normalizedValue, companyName);
    }

    private static boolean containsOnlyGenericWords(String value) {
        String normalized = value.toLowerCase(Locale.ROOT);
        return normalized.equals("职位")
                || normalized.equals("岗位")
                || normalized.equals("招聘岗位")
                || normalized.equals("职位名称")
                || normalized.equals("校招岗位")
                || normalized.equals("应届生")
                || normalized.equals("实习生");
    }

    private static boolean looksLikeCompanyEcho(String title, String companyName) {
        if (!StringUtils.hasText(companyName)) {
            return false;
        }

        String normalizedTitle = stripCompanySuffix(title);
        String normalizedCompany = stripCompanySuffix(companyName);
        if (!StringUtils.hasText(normalizedTitle) || !StringUtils.hasText(normalizedCompany)) {
            return false;
        }

        return normalizedTitle.equalsIgnoreCase(normalizedCompany)
                || normalizedTitle.startsWith(normalizedCompany)
                || normalizedCompany.startsWith(normalizedTitle);
    }

    private static String stripCompanySuffix(String value) {
        return normalizeInlineText(value)
                .replaceAll("(有限责任公司|股份有限公司|有限公司|集团|科技|信息技术|软件|网络|公司)$", "")
                .replaceAll("[（）()\\-_/路,.，。\\s]", "");
    }

    private static String extractAdministrativeSummary(String address) {
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (char currentChar : address.toCharArray()) {
            if (Character.isWhitespace(currentChar)) {
                continue;
            }
            current.append(currentChar);

            if (isAdministrativeBoundary(currentChar)) {
                String segment = current.toString().trim();
                if (StringUtils.hasText(segment)) {
                    segments.add(segment);
                }
                current.setLength(0);
                if (segments.size() >= 3) {
                    break;
                }
            }

            if (current.length() >= DEFAULT_ADDRESS_PREVIEW_LIMIT) {
                break;
            }
        }

        String summary = String.join("", segments);
        return shorten(summary, DEFAULT_ADDRESS_PREVIEW_LIMIT);
    }

    private static boolean isAdministrativeBoundary(char value) {
        return "省市州盟区县旗".indexOf(value) >= 0;
    }

    private static String takeFirstSegment(String address) {
        String[] segments = address.split("[,，、]");
        if (segments.length == 0) {
            return address;
        }
        return normalizeInlineText(segments[0]);
    }

    private static String shorten(String value, int limit) {
        String normalizedValue = normalizeInlineText(value);
        if (!StringUtils.hasText(normalizedValue) || normalizedValue.length() <= limit) {
            return normalizedValue;
        }
        return normalizedValue.substring(0, limit).trim();
    }

    private static boolean containsChinese(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (char currentChar : value.toCharArray()) {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(currentChar);
            if (block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A
                    || block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B) {
                return true;
            }
        }
        return false;
    }
}
