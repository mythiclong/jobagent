package com.zhihang.jobagent.config;

import com.zhihang.jobagent.entity.UserRole;
import com.zhihang.jobagent.service.UserAccountService;
import com.zhihang.jobagent.support.AuthenticatedUser;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Configuration
@EnableConfigurationProperties(DemoAccountProperties.class)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            AuthenticationSuccessHandler authenticationSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/about", "/login", "/register", "/forbidden", "/error").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/admin/**", "/jobs/**", "/question-bank/**", "/h2-console/**").hasRole(UserRole.ADMIN.name())
                        .requestMatchers("/career-profile/**", "/profile-center/**", "/profiles/**", "/match/**",
                                "/resume-review/**", "/resume-enhance/**", "/resume-optimize/**",
                                "/learning-path/**", "/strength-plan/**", "/interview/**",
                                "/history-center/**", "/my-records/**", "/records/**", "/history/**")
                        .hasAnyRole(UserRole.USER.name(), UserRole.ADMIN.name())
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .accessDeniedPage("/forbidden")
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint("/login"))
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(UNAUTHORIZED),
                                new AntPathRequestMatcher("/api/**")
                        )
                )
                .csrf(csrf -> csrf.ignoringRequestMatchers(new AntPathRequestMatcher("/h2-console/**")))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    AuthenticationSuccessHandler authenticationSuccessHandler(UserAccountService userAccountService) {
        HttpSessionRequestCache requestCache = new HttpSessionRequestCache();
        return new AuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                                                HttpServletResponse response,
                                                org.springframework.security.core.Authentication authentication)
                    throws IOException, ServletException {
                if (authentication.getPrincipal() instanceof AuthenticatedUser authenticatedUser) {
                    userAccountService.updateLastLoginAt(authenticatedUser.getId());
                    SavedRequest savedRequest = requestCache.getRequest(request, response);
                    if (savedRequest != null) {
                        String requestedPath = extractApplicationPath(savedRequest.getRedirectUrl(), request);
                        if (isPathAllowedForRole(requestedPath, authenticatedUser.getRole())) {
                            response.sendRedirect(request.getContextPath() + requestedPath);
                            return;
                        }
                    }
                    response.sendRedirect(request.getContextPath() + resolveLandingPath(authenticatedUser.getRole()));
                    return;
                }
                response.sendRedirect(request.getContextPath() + "/");
            }
        };
    }

    private String resolveLandingPath(UserRole role) {
        return role == UserRole.ADMIN ? "/admin" : "/career-profile";
    }

    private boolean isPathAllowedForRole(String path, UserRole role) {
        if (role == UserRole.ADMIN) {
            return true;
        }
        return !(path.startsWith("/admin") || path.startsWith("/jobs") || path.startsWith("/h2-console"));
    }

    private String extractApplicationPath(String redirectUrl, HttpServletRequest request) {
        var uri = UriComponentsBuilder.fromUriString(redirectUrl).build();
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return resolveLandingPath(UserRole.USER);
        }
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
            path = path + "?" + uri.getQuery();
        }
        return path;
    }
}
