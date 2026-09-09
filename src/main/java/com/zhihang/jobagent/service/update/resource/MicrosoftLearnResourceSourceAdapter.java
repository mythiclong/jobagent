package com.zhihang.jobagent.service.update.resource;

import com.zhihang.jobagent.service.update.ExternalFetchService;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class MicrosoftLearnResourceSourceAdapter extends AbstractDocumentResourceAdapter {

    public MicrosoftLearnResourceSourceAdapter(
            ExternalFetchService externalFetchService,
            @Value("${jobagent.update.resources.microsoft.enabled:false}") boolean enabled,
            @Value("${jobagent.update.resources.microsoft.source-name:MICROSOFT_LEARN}") String sourceName,
            @Value("${jobagent.update.resources.microsoft.url:https://learn.microsoft.com/en-us/training/browse/}") String url) {
        super(externalFetchService, enabled, sourceName, url);
    }

    @Override
    public List<FetchedLearningResourceItem> fetchResources() throws IOException {
        Document document = fetchDocument();
        return parseLinks(document, "a[href*='/training/modules/'], a[href*='/training/paths/']", 24);
    }
}
