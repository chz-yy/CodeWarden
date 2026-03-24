# CodeWarden

一个基于 Spring Boot + LangChain4j 的 Git Diff 自动代码审查后端服务。

它可以接收 `git diff` 文本，执行两层审查：

- **规则审查**：本地静态规则（如 TODO/FIXME、调试输出、疑似密钥、超长行）
- **Agent 审查**：通过 LangChain4j 调用大模型，给出更智能的风险与修复建议

---

## 技术栈

- Java 17
- Spring Boot 3
- LangChain4j
- Maven

---

## 项目结构

```text
src/main/java/com/code/codewarden
├─ controller
│  └─ ReviewController.java
├─ service
│  ├─ GitDiffReviewService.java
│  └─ CodeReviewAgentService.java
├─ config
│  └─ LangChain4jConfig.java
├─ DTO
│  ├─ ReviewRequest.java
│  ├─ ReviewResponse.java
│  └─ Issue.java
└─ exception
   └─ GlobalExceptionHandler.java
```

---

## 快速开始

### 1) 环境要求

- JDK 17+
- Maven Wrapper（项目已自带 `mvnw`）

> 注意：Spring Boot 3 需要 Java 17。若使用 Java 8 会出现 class version 错误（52 vs 61）。

### 2) 配置大模型参数

编辑 `src/main/resources/application.properties`：

```properties
spring.application.name=CodeWarden

# LangChain4j / OpenAI-compatible config
codewarden.llm.enabled=true
codewarden.llm.api-key=YOUR_API_KEY
codewarden.llm.base-url=https://api.openai.com/v1
codewarden.llm.model=gpt-4o-mini
codewarden.llm.temperature=0.1
codewarden.llm.timeout-seconds=60
```

如果你使用兼容 OpenAI 的模型服务（如自建网关/第三方平台），只需替换：

- `codewarden.llm.base-url`
- `codewarden.llm.model`
- `codewarden.llm.api-key`

### 3) 启动服务

Windows PowerShell:

```powershell
.\mvnw spring-boot:run
```

或先测试：

```powershell
.\mvnw test
```

---

## API 说明

### 健康检查

- `GET /api/review/health`

响应：

```text
ok
```

### 提交审查任务

- `POST /api/review`
- `Content-Type: application/json`

请求体：

```json
{
  "diffContent": "diff --git a/src/A.java b/src/A.java\n+++ b/src/A.java\n@@ -1,1 +1,2 @@\n+System.out.println(\"debug\");\n+String token = \"abc\";",
  "repoUrl": "https://github.com/example/repo",
  "branch": "feature/review-agent"
}
```

字段说明：

- `diffContent`：完整 unified diff 字符串（必填）
- `repoUrl`：仓库地址（可选）
- `branch`：分支名（可选）

返回体示例：

```json
{
  "success": true,
  "issues": [
    {
      "severity": "MEDIUM",
      "rule": "DEBUG_OUTPUT",
      "filePath": "src/A.java",
      "line": 2,
      "message": "检测到调试输出语句，可能污染生产日志。",
      "suggestion": "使用正式日志框架（如 slf4j）并控制日志级别。"
    },
    {
      "severity": "HIGH",
      "rule": "POTENTIAL_SECRET",
      "filePath": "src/A.java",
      "line": 3,
      "message": "疑似硬编码敏感信息（密码/密钥/token）。",
      "suggestion": "改为环境变量或配置中心注入，避免明文提交。"
    }
  ],
  "summary": "审查完成：发现 2 个问题。branch=feature/review-agent, repo=https://github.com/example/repo。LLM: ..."
}
```

---

## 当前内置规则

`GitDiffReviewService` 对新增行进行以下检查：

- `COMMENT_TODO_FIXME`：检测 TODO/FIXME
- `DEBUG_OUTPUT`：检测 `System.out.print` / `printStackTrace` / `console.log`
- `POTENTIAL_SECRET`：检测疑似硬编码敏感信息
- `LINE_TOO_LONG`：检测超长行（>140）

---

## LLM 审查策略

`CodeReviewAgentService` 会把 diff 发送给模型，并要求返回 JSON：

- `summary`: 一句话总结
- `issues`: 问题列表（severity/rule/filePath/line/message/suggestion）

容错策略：

- 未配置 API Key：自动跳过 LLM 审查，仅返回规则审查结果
- LLM 调用失败或返回格式异常：自动降级，不影响接口可用性

---

## 常见问题

### 1) 启动时报 class version 错误

请确认：

- `java -version` 输出为 17 或更高
- `JAVA_HOME` 指向 JDK 17+

### 2) LLM 没有生效

请检查：

- `codewarden.llm.enabled=true`
- `codewarden.llm.api-key` 已填写
- `codewarden.llm.base-url` 与 `codewarden.llm.model` 匹配你的提供商

---

## 后续可扩展方向

- 支持 GitHub/GitLab Webhook 自动触发审查
- 支持多提供商切换（OpenAI/DeepSeek/通义等）
- 按语言分审查策略（Java/JS/Go）
- 审查结果持久化（数据库）与历史查询
- 增加 PR 评论回写能力

