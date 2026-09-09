package com.zhihang.jobagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jobagent.demo")
public class DemoAccountProperties {

    private String adminUsername = "admin";
    private String adminPassword = "123456";
    private String adminDisplayName = "\u7cfb\u7edf\u7ba1\u7406\u5458";
    private String adminEmail = "admin@example.com";
    private String userUsername = "demo";
    private String userPassword = "123456";
    private String userDisplayName = "\u6f14\u793a\u6c42\u804c\u8005";
    private String userEmail = "demo@example.com";
    private boolean seedDemoProfile = true;

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getAdminDisplayName() {
        return adminDisplayName;
    }

    public void setAdminDisplayName(String adminDisplayName) {
        this.adminDisplayName = adminDisplayName;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public String getUserUsername() {
        return userUsername;
    }

    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public String getUserDisplayName() {
        return userDisplayName;
    }

    public void setUserDisplayName(String userDisplayName) {
        this.userDisplayName = userDisplayName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public boolean isSeedDemoProfile() {
        return seedDemoProfile;
    }

    public void setSeedDemoProfile(boolean seedDemoProfile) {
        this.seedDemoProfile = seedDemoProfile;
    }
}
