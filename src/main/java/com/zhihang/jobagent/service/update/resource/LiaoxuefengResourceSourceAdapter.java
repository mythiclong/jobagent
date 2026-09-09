package com.zhihang.jobagent.service.update.resource;

import com.zhihang.jobagent.service.update.ExternalFetchService;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class LiaoxuefengResourceSourceAdapter extends AbstractDocumentResourceAdapter {

    public LiaoxuefengResourceSourceAdapter(
            ExternalFetchService externalFetchService,
            @Value("${jobagent.update.resources.liaoxuefeng.enabled:true}") boolean enabled,
            @Value("${jobagent.update.resources.liaoxuefeng.source-name:LIAOXUEFENG}") String sourceName,
            @Value("${jobagent.update.resources.liaoxuefeng.url:https://www.liaoxuefeng.com/}") String url) {
        super(externalFetchService, enabled, sourceName, url);
    }

    @Override
    public List<FetchedLearningResourceItem> fetchResources() throws IOException {
        Document document = fetchDocument();
        return parseLinks(document, "a[href^='/books/']", 24);
    }
}
