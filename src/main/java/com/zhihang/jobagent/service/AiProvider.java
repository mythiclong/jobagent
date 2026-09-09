package com.zhihang.jobagent.service;

import com.zhihang.jobagent.dto.GenerationSource;

public interface AiProvider {

    GenerationSource source();

    boolean isEnabled();

    String getConfiguredModelName();

    String getApiKeySource();

    int getTimeoutSeconds();

    AiProviderResult generate(String action, String prompt, float temperature);
}
