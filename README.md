# jobagent

一个基于 Spring Boot 3、Thymeleaf 和 Spring Data JPA 的求职准备与 AI 辅助系统。

项目覆盖个人求职档案管理、岗位信息整理与匹配、简历分析与优化、模拟面试和学习路径建议等功能，并支持 Gemini、Qwen 多通道调用与本地规则兜底。

## 项目背景

本人于 2026 年 3 月 23 日左右开始开发本项目。目前计划将它作为申请 AI Agent 开发实习岗位的 Demo 和求职作品，用于展示后端开发、业务建模、AI 能力编排以及完整应用落地能力。

## 技术栈

Spring Boot 3、Spring MVC、Thymeleaf、Spring Data JPA、Spring Security、H2/MySQL、Gemini/Qwen。

## 本地运行

环境要求：JDK 17。

```powershell
.\mvnw.cmd spring-boot:run
```

运行测试：

```powershell
.\mvnw.cmd test
```

## 特别鸣谢

特别感谢 Codex、GPT-5.5 和 GPT-5.6 Luna 在本项目设计、开发与完善过程中的协助。
