package com.zhihang.jobagent.service;

import com.zhihang.jobagent.dto.ViewerContext;
import com.zhihang.jobagent.entity.UserAccount;
import com.zhihang.jobagent.entity.UserRole;
import com.zhihang.jobagent.support.AuthenticatedUser;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CurrentUserService {

    private final UserAccountService userAccountService;

    public CurrentUserService(UserAccountService userAccountService) {
        this.userAccountService = userAccountService;
    }

    public Optional<AuthenticatedUser> findAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser authenticatedUser) {
            return Optional.of(authenticatedUser);
        }
        if (principal instanceof UserDetails userDetails) {
            return userAccountService.findByUsername(userDetails.getUsername())
                    .map(AuthenticatedUser::new);
        }
        if (principal instanceof String username && !"anonymousUser".equalsIgnoreCase(username)) {
            return userAccountService.findByUsername(username)
                    .map(AuthenticatedUser::new);
        }
        return Optional.empty();
    }

    public boolean isAuthenticated() {
        return findAuthenticatedUser().isPresent();
    }

    public boolean isAdmin() {
        return findAuthenticatedUser()
                .map(AuthenticatedUser::isAdmin)
                .orElse(false);
    }

    public UserAccount requireCurrentUserAccount() {
        AuthenticatedUser authenticatedUser = findAuthenticatedUser()
                .orElseThrow(() -> new IllegalStateException("Current request has no authenticated user."));
        return userAccountService.getRequired(authenticatedUser.getId());
    }

    public Long requireCurrentUserId() {
        return requireCurrentUserAccount().getId();
    }

    public ViewerContext buildViewerContext() {
        return findAuthenticatedUser()
                .map(user -> new ViewerContext(
                        true,
                        user.getRole() == UserRole.ADMIN,
                        user.getRole() == UserRole.USER,
                        user.getId(),
                        user.getUsername(),
                        user.getDisplayName(),
                        user.getRole().name(),
                        user.getRole() == UserRole.ADMIN ? "/admin" : "/career-profile"
                ))
                .orElseGet(ViewerContext::guest);
    }
}
