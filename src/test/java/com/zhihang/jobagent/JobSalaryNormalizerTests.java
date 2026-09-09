package com.zhihang.jobagent;

import com.zhihang.jobagent.entity.JobPost;
import com.zhihang.jobagent.support.JobSalaryNormalizer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class JobSalaryNormalizerTests {

    @Test
    void shouldTreatSmallMonthlyRangeAsThousands() {
        JobSalaryNormalizer.SalaryNormalization normalization = JobSalaryNormalizer.normalize(
                "3",
                "5",
                new BigDecimal("3"),
                new BigDecimal("5")
        );

        assertThat(normalization.unitType()).isEqualTo("MONTH");
        assertThat(normalization.monthlyComparableMin()).isEqualByComparingTo("3000");
        assertThat(normalization.monthlyComparableMax()).isEqualByComparingTo("5000");
        assertThat(normalization.displayText()).isEqualTo("3k-5k/月");
    }

    @Test
    void shouldRepairLegacyMonthlyDisplayAndComparableValues() {
        JobPost jobPost = new JobPost();
        jobPost.setSalaryMin(new BigDecimal("3"));
        jobPost.setSalaryMax(new BigDecimal("5"));
        jobPost.setSalaryUnitType("MONTH");
        jobPost.setSalaryDisplayText("3 元/月 - 5 元/月");

        assertThat(jobPost.getSalaryRangeDisplay()).isEqualTo("3k-5k/月");
        assertThat(jobPost.getMonthlySalaryMin()).isEqualByComparingTo("3000");
        assertThat(jobPost.getMonthlySalaryMax()).isEqualByComparingTo("5000");
    }
}
