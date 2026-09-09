package com.zhihang.jobagent.dto;

public record ViewerContext(boolean authenticated,
                            boolean admin,
                            boolean user,
                            Long userId,
                            String username,
                            String displayName,
                            String role,
                            String landingPath) {

    public static ViewerContext guest() {
        return new ViewerContext(false, false, false, null, "", "", "GUEST", "/");
    }
}
