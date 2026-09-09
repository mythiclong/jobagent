# jobagent

基于 `Spring Boot 3 + Thymeleaf + Spring Data JPA` 的求职准备系统（默认使用本地 H2 文件库）。

## 环境要求

- JDK 17
- Windows / PowerShell（项目自带 `mvnw.cmd`，无需本地安装 Maven）

## 本地启动

```powershell
.\mvnw.cmd spring-boot:run
```

启动后访问：

- 应用首页：`http://localhost:8081/`
- H2 Console：`http://localhost:8081/h2-console`

H2 连接信息：

- JDBC URL: `jdbc:h2:file:./data/jobagent`
- User Name: `sa`
- Password: 空

## 常用命令

```powershell
.\mvnw.cmd test
.\mvnw.cmd -DskipTests package
java -jar .\target\jobagent-0.0.1-SNAPSHOT.jar
```

## AI 通道配置（环境变量优先）

当前 AI 链路是三层容灾：

1. Gemini（主通道）
2. Qwen3.5-Plus（备用通道）
3. 本地规则（兜底）

### Gemini

- 推荐环境变量：`GEMINI_API_KEY`
- 兼容属性键：`jobagent.ai.gemini.api-key`（仅过渡，不建议存真实 key）

### Qwen

- 推荐环境变量：`DASHSCOPE_API_KEY`
- 兼容环境变量：`QWEN_API_KEY`
- 兼容属性键：`jobagent.ai.qwen.api-key`（仅过渡，不建议存真实 key）

PowerShell 示例：

```powershell
$env:GEMINI_API_KEY="your-gemini-key"
$env:DASHSCOPE_API_KEY="your-qwen-key"
.\mvnw.cmd spring-boot:run
```

管理员连通性测试页：

- `http://localhost:8081/admin/ai-test`

该页面可显示：

- Gemini/Qwen 配置状态
- 最终命中来源（Gemini / Qwen / Fallback）
- 模型名
- 主通道失败原因
- 备用通道失败原因
- 是否超时

## 目录结构

- `src/main/java/.../controller`：页面路由与表单提交
- `src/main/java/.../service`：业务逻辑与 AI 编排
- `src/main/java/.../entity`：JPA 实体
- `src/main/java/.../repository`：数据访问层
- `src/main/resources/templates`：Thymeleaf 模板
- `src/main/resources/application.properties`：默认配置
