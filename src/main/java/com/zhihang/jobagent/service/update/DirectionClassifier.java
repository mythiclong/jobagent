package com.zhihang.jobagent.service.update;

public final class DirectionClassifier {

    private DirectionClassifier() {
    }

    public static String classifyDirection(String title, String summary, String extraText) {
        String joined = (UpdateSourceSupport.defaultString(title) + " "
                + UpdateSourceSupport.defaultString(summary) + " "
                + UpdateSourceSupport.defaultString(extraText)).toLowerCase();

        if (containsAny(joined, "java", "spring", "mysql", "mybatis", "rest", "api", "后端", "backend")) {
            return "Java后端";
        }
        if (containsAny(joined, "react", "vue", "javascript", "typescript", "html", "css", "前端", "frontend")) {
            return "前端开发";
        }
        if (containsAny(joined, "产品", "prd", "原型", "竞品", "需求分析", "product manager", "pm")) {
            return "产品经理";
        }
        if (containsAny(joined, "测试", "qa", "selenium", "jmeter", "测试开发", "quality assurance")) {
            return "测试开发";
        }
        if (containsAny(joined, "数据分析", "python", "pandas", "sql", "bi", "excel", "数据")) {
            return "数据分析";
        }
        return "通用方向";
    }

    public static String classifyQuestionTag(String title, String repoName) {
        String joined = (UpdateSourceSupport.defaultString(title) + " " + UpdateSourceSupport.defaultString(repoName)).toLowerCase();
        if (containsAny(joined, "java", "spring")) {
            return joined.contains("spring") ? "Spring" : "Java";
        }
        if (containsAny(joined, "sql", "mysql", "database")) {
            return "SQL / 数据库";
        }
        if (containsAny(joined, "react")) {
            return "React";
        }
        if (containsAny(joined, "javascript", "typescript", "frontend")) {
            return "JavaScript";
        }
        if (containsAny(joined, "system design", "architecture")) {
            return "System Design";
        }
        if (containsAny(joined, "product", "prd", "pm")) {
            return "Product / PM";
        }
        return "Backend";
    }

    public static String classifyResourceType(String title, String summary) {
        String joined = (UpdateSourceSupport.defaultString(title) + " " + UpdateSourceSupport.defaultString(summary)).toLowerCase();
        if (containsAny(joined, "course", "课程", "tutorial", "教程")) {
            return "COURSE";
        }
        if (containsAny(joined, "guide", "learn", "docs", "documentation", "文档")) {
            return "GUIDE";
        }
        if (containsAny(joined, "project", "demo", "实战", "项目")) {
            return "PROJECT";
        }
        return "ARTICLE";
    }

    public static String classifyStageTag(String title, String summary) {
        String joined = (UpdateSourceSupport.defaultString(title) + " " + UpdateSourceSupport.defaultString(summary)).toLowerCase();
        if (containsAny(joined, "interview", "面试")) {
            return "INTERVIEW";
        }
        if (containsAny(joined, "project", "实战", "demo")) {
            return "PROJECT";
        }
        if (containsAny(joined, "beginner", "入门", "基础", "getting started")) {
            return "BEGINNER";
        }
        return "INTERMEDIATE";
    }

    private static boolean containsAny(String source, String... keywords) {
        for (String keyword : keywords) {
            if (source.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
