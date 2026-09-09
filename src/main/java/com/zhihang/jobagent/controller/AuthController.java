package com.zhihang.jobagent.controller;

import com.zhihang.jobagent.dto.RegistrationForm;
import com.zhihang.jobagent.service.CurrentUserService;
import com.zhihang.jobagent.service.UserAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final CurrentUserService currentUserService;
    private final UserAccountService userAccountService;
    private final AuthenticationManager authenticationManager;

    public AuthController(CurrentUserService currentUserService,
                          UserAccountService userAccountService,
                          AuthenticationManager authenticationManager) {
        this.currentUserService = currentUserService;
        this.userAccountService = userAccountService;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/login")
    public String showLoginPage(@RequestParam(required = false) String error,
                                @RequestParam(required = false) String logout,
                                Model model) {
        if (currentUserService.isAuthenticated()) {
            return "redirect:" + currentUserService.buildViewerContext().landingPath();
        }
        if (error != null) {
            model.addAttribute("loginNotice", "\u7528\u6237\u540d\u6216\u5bc6\u7801\u4e0d\u6b63\u786e\uff0c\u8bf7\u91cd\u65b0\u8f93\u5165\u3002");
        } else if (logout != null) {
            model.addAttribute("loginNotice", "\u4f60\u5df2\u5b89\u5168\u9000\u51fa\u767b\u5f55\u3002");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        if (currentUserService.isAuthenticated()) {
            return "redirect:" + currentUserService.buildViewerContext().landingPath();
        }
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegistrationForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@ModelAttribute("registerForm") RegistrationForm registerForm,
                           BindingResult bindingResult,
                           HttpServletRequest request,
                           HttpServletResponse response) {
        validateRegistrationForm(registerForm, bindingResult);
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userAccountService.registerUser(
                    registerForm.getUsername(),
                    registerForm.getDisplayName(),
                    registerForm.getEmail(),
                    registerForm.getPassword()
            );
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("username", "register.username", ex.getMessage());
            return "auth/register";
        }

        authenticateAfterRegistration(registerForm.getUsername(), registerForm.getPassword(), request, response);
        return "redirect:/career-profile";
    }

    private void validateRegistrationForm(RegistrationForm registerForm, BindingResult bindingResult) {
        if (!StringUtils.hasText(registerForm.getUsername())) {
            bindingResult.rejectValue("username", "register.username", "\u8bf7\u8f93\u5165\u767b\u5f55\u540d\u3002");
        }
        if (!StringUtils.hasText(registerForm.getDisplayName())) {
            bindingResult.rejectValue("displayName", "register.displayName", "\u8bf7\u8f93\u5165\u4f60\u7684\u79f0\u547c\u3002");
        }
        if (!StringUtils.hasText(registerForm.getPassword())) {
            bindingResult.rejectValue("password", "register.password", "\u8bf7\u8f93\u5165\u767b\u5f55\u5bc6\u7801\u3002");
        }
        if (!StringUtils.hasText(registerForm.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "register.confirmPassword", "\u8bf7\u518d\u6b21\u8f93\u5165\u5bc6\u7801\u3002");
        }
        if (StringUtils.hasText(registerForm.getPassword())
                && StringUtils.hasText(registerForm.getConfirmPassword())
                && !registerForm.getPassword().equals(registerForm.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "register.confirmPassword", "\u4e24\u6b21\u8f93\u5165\u7684\u5bc6\u7801\u4e0d\u4e00\u81f4\u3002");
        }
    }

    private void authenticateAfterRegistration(String username,
                                               String password,
                                               HttpServletRequest request,
                                               HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(username, password)
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        new HttpSessionSecurityContextRepository().saveContext(securityContext, request, response);
    }
}
