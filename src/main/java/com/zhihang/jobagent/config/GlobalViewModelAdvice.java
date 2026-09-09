package com.zhihang.jobagent.config;

import com.zhihang.jobagent.dto.ViewerContext;
import com.zhihang.jobagent.service.CurrentUserService;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalViewModelAdvice {

    private final CurrentUserService currentUserService;

    public GlobalViewModelAdvice(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @ModelAttribute("viewer")
    public ViewerContext viewer() {
        return currentUserService.buildViewerContext();
    }
}
