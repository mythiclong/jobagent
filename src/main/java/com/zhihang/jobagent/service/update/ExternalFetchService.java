package com.zhihang.jobagent.service.update;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;

@Service
public class ExternalFetchService {

    private static final Logger log = LoggerFactory.getLogger(ExternalFetchService.class);

    private final int timeoutSeconds;

    public ExternalFetchService(@Value("${jobagent.update-center.fetch-timeout-seconds:20}") int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Document fetchDocument(String url) throws IOException {
        log.info("Fetching external HTML: {}", url);
        return Jsoup.connect(url)
                .userAgent("jobagent-demo/1.0")
                .timeout((int) Duration.ofSeconds(timeoutSeconds).toMillis())
                .followRedirects(true)
                .get();
    }

    public String fetchText(String url) throws IOException {
        log.info("Fetching external text: {}", url);
        return Jsoup.connect(url)
                .userAgent("jobagent-demo/1.0")
                .ignoreContentType(true)
                .timeout((int) Duration.ofSeconds(timeoutSeconds).toMillis())
                .followRedirects(true)
                .execute()
                .body();
    }

    public HttpClient newHttpClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
