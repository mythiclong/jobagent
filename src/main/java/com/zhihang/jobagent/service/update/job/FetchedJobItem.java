package com.zhihang.jobagent.service.update.job;

public record FetchedJobItem(
        String sourceName,
        String title,
        String companyName,
        String location,
        String publishDateText,
        String externalUrl,
        String summary,
        String direction,
        String externalKey
) {
}
