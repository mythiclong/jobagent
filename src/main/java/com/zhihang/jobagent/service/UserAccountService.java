package com.zhihang.jobagent.service;

import com.zhihang.jobagent.entity.UserAccount;
import com.zhihang.jobagent.entity.UserRole;
import com.zhihang.jobagent.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserAccountService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository userAccountRepository,
                              PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserAccount registerUser(String username,
                                    String displayName,
                                    String email,
                                    String rawPassword) {
        String normalizedUsername = normalizeUsername(username);
        if (!StringUtils.hasText(normalizedUsername)) {
            throw new IllegalArgumentException("\u8bf7\u8f93\u5165\u7528\u6237\u540d\u3002");
        }
        if (!StringUtils.hasText(rawPassword) || rawPassword.trim().length() < 6) {
            throw new IllegalArgumentException("\u5bc6\u7801\u81f3\u5c11\u9700\u8981 6 \u4f4d\u3002");
        }
        if (userAccountRepository.existsByUsername(normalizedUsername)) {
            throw new IllegalArgumentException("\u7528\u6237\u540d\u5df2\u88ab\u5360\u7528\uff0c\u8bf7\u66f4\u6362\u540e\u518d\u8bd5\u3002");
        }

        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(normalizedUsername);
        userAccount.setPasswordHash(passwordEncoder.encode(rawPassword));
        userAccount.setDisplayName(defaultDisplayName(displayName, normalizedUsername));
        userAccount.setEmail(normalizeEmail(email));
        userAccount.setRole(UserRole.USER);
        userAccount.setEnabled(true);
        return userAccountRepository.save(userAccount);
    }

    @Transactional
    public UserAccount createAccountIfMissing(String username,
                                              String displayName,
                                              String email,
                                              String rawPassword,
                                              UserRole role) {
        String normalizedUsername = normalizeUsername(username);
        return userAccountRepository.findByUsername(normalizedUsername)
                .orElseGet(() -> {
                    UserAccount userAccount = new UserAccount();
                    userAccount.setUsername(normalizedUsername);
                    userAccount.setPasswordHash(passwordEncoder.encode(rawPassword));
                    userAccount.setDisplayName(defaultDisplayName(displayName, normalizedUsername));
                    userAccount.setEmail(normalizeEmail(email));
                    userAccount.setRole(role);
                    userAccount.setEnabled(true);
                    return userAccountRepository.save(userAccount);
                });
    }

    @Transactional
    public UserAccount upsertSeedAccount(String username,
                                         String displayName,
                                         String email,
                                         String rawPassword,
                                         UserRole role) {
        String normalizedUsername = normalizeUsername(username);
        String normalizedDisplayName = defaultDisplayName(displayName, normalizedUsername);
        String normalizedEmail = normalizeEmail(email);

        return userAccountRepository.findByUsername(normalizedUsername)
                .map(existing -> updateSeedAccount(existing, normalizedDisplayName, normalizedEmail, rawPassword, role))
                .orElseGet(() -> createAccountIfMissing(normalizedUsername, normalizedDisplayName, normalizedEmail, rawPassword, role));
    }

    @Transactional
    public void updateLastLoginAt(Long userId) {
        if (userId == null) {
            return;
        }
        userAccountRepository.findById(userId).ifPresent(userAccount -> {
            userAccount.setLastLoginAt(LocalDateTime.now());
            userAccountRepository.save(userAccount);
        });
    }

    public UserAccount getRequired(Long userId) {
        return userAccountRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("\u7528\u6237\u8d26\u53f7\u4e0d\u5b58\u5728: " + userId));
    }

    public UserAccount getRequiredByUsername(String username) {
        return userAccountRepository.findByUsername(normalizeUsername(username))
                .orElseThrow(() -> new IllegalArgumentException("\u7528\u6237\u8d26\u53f7\u4e0d\u5b58\u5728: " + username));
    }

    public Optional<UserAccount> findByUsername(String username) {
        return userAccountRepository.findByUsername(normalizeUsername(username));
    }

    public String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return email.trim();
    }

    private String defaultDisplayName(String displayName, String username) {
        if (StringUtils.hasText(displayName)) {
            return displayName.trim();
        }
        return username;
    }

    private UserAccount updateSeedAccount(UserAccount existing,
                                          String displayName,
                                          String email,
                                          String rawPassword,
                                          UserRole role) {
        boolean changed = false;

        if (!Objects.equals(existing.getDisplayName(), displayName)) {
            existing.setDisplayName(displayName);
            changed = true;
        }
        if (!Objects.equals(existing.getEmail(), email)) {
            existing.setEmail(email);
            changed = true;
        }
        if (existing.getRole() != role) {
            existing.setRole(role);
            changed = true;
        }
        if (!existing.isEnabled()) {
            existing.setEnabled(true);
            changed = true;
        }
        if (!StringUtils.hasText(existing.getPasswordHash()) || !passwordEncoder.matches(rawPassword, existing.getPasswordHash())) {
            existing.setPasswordHash(passwordEncoder.encode(rawPassword));
            changed = true;
        }

        if (changed) {
            return userAccountRepository.save(existing);
        }
        return existing;
    }
}
