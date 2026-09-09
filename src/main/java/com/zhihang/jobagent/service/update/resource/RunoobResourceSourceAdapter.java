package com.zhihang.jobagent.service.update.resource;

import com.zhihang.jobagent.service.update.ExternalFetchService;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class RunoobResourceSourceAdapter extends AbstractDocumentResourceAdapter {

    public RunoobResourceSourceAdapter(
            ExternalFetchService externalFetchService,
            @Value("${jobagent.update.resources.runoob.enabled:true}") boolean enabled,
            @Value("${jobagent.update.resources.runoob.source-name:RUNOOB}") String sourceName,
            @Value("${jobagent.update.resources.runoob.url:https://www.runoob.com/}") String url) {
        super(externalFetchService, enabled, sourceName, url);
    }

    @Override
    public List<FetchedLearningResourceItem> fetchResources() throws IOException {
        Document document = fetchDocument();
        return parseLinks(document, "a.item-top", 40);
    }
}
