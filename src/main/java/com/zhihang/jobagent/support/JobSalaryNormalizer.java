package com.zhihang.jobagent.support;

import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class JobSalaryNormalizer {

    public static final String UNIT_MONTH = "MONTH";
    public static final String UNIT_DAY = "DAY";
    public static final String UNIT_HOUR = "HOUR";
    public static final String UNIT_UNKNOWN = "UNKNOWN";
    public static final String NEGOTIABLE_TEXT = "面议";

    private static final BigDecimal THOUSAND = new BigDecimal("1000");
    private static final BigDecimal MONTH_SHORTHAND_MAX = new BigDecimal("100");
    private static final BigDecimal UNKNOWN_UNIT_MAX = new BigDecimal("1000");

    private JobSalaryNormalizer() {
    }

    public static SalaryNormalization normalize(String rawMinText,
                                                String rawMaxText,
                                                BigDecimal parsedMin,
                                                BigDecimal parsedMax) {
        String rawText = buildRawSalaryText(rawMinText, rawMaxText);
        if (!StringUtils.hasText(rawText) && parsedMin == null && parsedMax == null) {
            return SalaryNormalization.empty();
        }

        if (containsNegotiable(rawText)) {
            return new SalaryNormalization(rawText, NEGOTIABLE_TEXT, UNIT_UNKNOWN, null, null);
        }

        BigDecimal min = parsedMin;
        BigDecimal max = parsedMax;
        if (min != null && max != null && min.compareTo(max) > 0) {
            BigDecimal swapped = min;
            min = max;
            max = swapped;
        }

        String resolvedUnit = detectExplicitUnit(rawText);
        if (!StringUtils.hasText(resolvedUnit)) {
            resolvedUnit = inferUnitByAmount(min, max);
        }

        BigDecimal comparableMin = null;
        BigDecimal comparableMax = null;
        if (UNIT_MONTH.equals(resolvedUnit)) {
            comparableMin = normalizeStoredMonthlyComparableAmount(min);
            comparableMax = normalizeStoredMonthlyComparableAmount(max);
        }

        String displayText = switch (resolvedUnit) {
            case UNIT_MONTH -> formatMonthlyRange(comparableMin, comparableMax, rawText);
            case UNIT_DAY -> formatCycleRange(min, max, "元/天", rawText);
            case UNIT_HOUR -> formatCycleRange(min, max, "元/小时", rawText);
            default -> formatUnknownRange(min, max, rawText);
        };

        return new SalaryNormalization(
                rawText,
                displayText,
                normalizeUnitType(resolvedUnit),
                comparableMin,
                comparableMax
        );
    }

    public static String normalizeUnitType(String unitType) {
        if (!StringUtils.hasText(unitType)) {
            return UNIT_UNKNOWN;
        }
        String normalized = unitType.trim().toUpperCase();
        return switch (normalized) {
            case UNIT_MONTH, UNIT_DAY, UNIT_HOUR -> normalized;
            default -> UNIT_UNKNOWN;
        };
    }

    public static boolean isMonthlyUnit(String unitType) {
        return UNIT_MONTH.equals(normalizeUnitType(unitType));
    }

    public static BigDecimal normalizeStoredMonthlyComparableAmount(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        if (amount.abs().compareTo(MONTH_SHORTHAND_MAX) <= 0) {
            return amount.multiply(THOUSAND);
        }
        return amount;
    }

    public static String formatMonthlyDisplayAmount(BigDecimal amount) {
        if (amount == null) {
            return "";
        }

        BigDecimal normalizedAmount = normalizeStoredMonthlyComparableAmount(amount);
        if (normalizedAmount.abs().compareTo(THOUSAND) >= 0) {
            BigDecimal thousandValue = normalizedAmount.divide(THOUSAND, 2, RoundingMode.HALF_UP);
            return thousandValue.stripTrailingZeros().toPlainString() + "k";
        }
        return normalizedAmount.stripTrailingZeros().toPlainString() + "元";
    }

    public record SalaryNormalization(String rawText,
                                      String displayText,
                                      String unitType,
                                      BigDecimal monthlyComparableMin,
                                      BigDecimal monthlyComparableMax) {

        public static SalaryNormalization empty() {
            return new SalaryNormalization("", "", UNIT_UNKNOWN, null, null);
        }
    }

    private static String buildRawSalaryText(String rawMinText, String rawMaxText) {
        String rawMin = JobDisplayFormatter.normalizeInlineText(rawMinText);
        String rawMax = JobDisplayFormatter.normalizeInlineText(rawMaxText);
        if (StringUtils.hasText(rawMin) && StringUtils.hasText(rawMax)) {
            if (rawMin.equals(rawMax)) {
                return rawMin;
            }
            return rawMin + "-" + rawMax;
        }
        if (StringUtils.hasText(rawMin)) {
            return rawMin;
        }
        return rawMax;
    }

    private static boolean containsNegotiable(String rawText) {
        return StringUtils.hasText(rawText) && rawText.contains(NEGOTIABLE_TEXT);
    }

    private static String detectExplicitUnit(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return "";
        }
        String normalized = rawText.toLowerCase();
        if (normalized.contains("小时") || normalized.contains("时薪") || normalized.contains("/h") || normalized.contains("/小时")) {
            return UNIT_HOUR;
        }
        if (normalized.contains("/天") || normalized.contains("/d") || normalized.contains("日薪") || normalized.contains("元/天") || normalized.contains("元/日")) {
            return UNIT_DAY;
        }
        if (normalized.contains("月") || normalized.contains("/月") || normalized.contains("月薪")
                || normalized.contains("k") || normalized.contains("千") || normalized.contains("万")) {
            return UNIT_MONTH;
        }
        return "";
    }

    private static String inferUnitByAmount(BigDecimal min, BigDecimal max) {
        BigDecimal baseline = maxAbs(min, max);
        if (baseline == null) {
            return UNIT_UNKNOWN;
        }
        if (baseline.compareTo(MONTH_SHORTHAND_MAX) <= 0) {
            return UNIT_MONTH;
        }
        if (baseline.compareTo(UNKNOWN_UNIT_MAX) <= 0) {
            return UNIT_UNKNOWN;
        }
        return UNIT_MONTH;
    }

    private static String formatMonthlyRange(BigDecimal min, BigDecimal max, String rawText) {
        if (min == null && max == null) {
            return fallback(rawText);
        }
        if (min != null && max != null && min.compareTo(max) > 0) {
            BigDecimal swapped = min;
            min = max;
            max = swapped;
        }
        if (min != null && max != null) {
            if (min.compareTo(max) == 0) {
                return formatMonthlyDisplayAmount(min) + "/月";
            }
            return formatMonthlyDisplayAmount(min) + "-" + formatMonthlyDisplayAmount(max) + "/月";
        }
        BigDecimal only = min != null ? min : max;
        return formatMonthlyDisplayAmount(only) + "/月";
    }

    private static String formatCycleRange(BigDecimal min, BigDecimal max, String suffix, String rawText) {
        if (min == null && max == null) {
            return fallback(rawText);
        }
        if (min != null && max != null) {
            if (min.compareTo(max) == 0) {
                return formatPlainAmount(min) + suffix;
            }
            return formatPlainAmount(min) + "-" + formatPlainAmount(max) + suffix;
        }
        BigDecimal only = min != null ? min : max;
        return formatPlainAmount(only) + suffix;
    }

    private static String formatUnknownRange(BigDecimal min, BigDecimal max, String rawText) {
        if (min != null || max != null) {
            String range = formatPlainRange(min, max);
            if (StringUtils.hasText(range)) {
                return range + "（短周期薪资，单位待识别）";
            }
        }
        return fallback(rawText);
    }

    private static String formatPlainRange(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return "";
        }
        if (min != null && max != null) {
            if (min.compareTo(max) == 0) {
                return formatPlainAmount(min);
            }
            return formatPlainAmount(min) + "-" + formatPlainAmount(max);
        }
        return formatPlainAmount(min != null ? min : max);
    }

    private static String formatPlainAmount(BigDecimal amount) {
        return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
    }

    private static BigDecimal maxAbs(BigDecimal min, BigDecimal max) {
        BigDecimal normalizedMin = min == null ? null : min.abs();
        BigDecimal normalizedMax = max == null ? null : max.abs();
        if (normalizedMin == null) {
            return normalizedMax;
        }
        if (normalizedMax == null) {
            return normalizedMin;
        }
        return normalizedMin.max(normalizedMax);
    }

    private static String fallback(String rawText) {
        return StringUtils.hasText(rawText) ? rawText : "-";
    }
}
