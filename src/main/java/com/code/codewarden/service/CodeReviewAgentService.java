package com.code.codewarden.service;

import com.code.codewarden.dto.Issue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class CodeReviewAgentService {
    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final boolean enabled;

    public CodeReviewAgentService(
            ChatLanguageModel chatLanguageModel,
            ObjectMapper objectMapper,
            @Value("${codewarden.llm.api-key:}") String apiKey,
            @Value("${codewarden.llm.enabled:true}") boolean enabled
    ) {
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.enabled = enabled;
    }

    public AgentReviewResult reviewDiff(String diffContent) {
        if (!enabled) {
            return new AgentReviewResult(Collections.emptyList(), "LLM 审查已关闭。");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return new AgentReviewResult(Collections.emptyList(), "未配置 LLM API Key，跳过 LLM 审查。");
        }
        if (diffContent == null || diffContent.isBlank()) {
            return new AgentReviewResult(Collections.emptyList(), "空 diff，无需 LLM 审查。");
        }

        String prompt = buildPrompt(diffContent);
        String raw;
        try {
            raw = chatLanguageModel.generate(prompt);
        } catch (Exception ex) {
            return new AgentReviewResult(Collections.emptyList(), "LLM 调用失败: " + ex.getMessage());
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            String summary = root.path("summary").asText("LLM 审查完成。");
            JsonNode issuesNode = root.path("issues");
            if (!issuesNode.isArray()) {
                return new AgentReviewResult(Collections.emptyList(), summary);
            }
            List<Issue> issues = objectMapper.convertValue(issuesNode, new TypeReference<List<Issue>>() {
            });
            sanitizeIssues(issues);
            return new AgentReviewResult(issues, summary);
        } catch (Exception ex) {
            return new AgentReviewResult(Collections.emptyList(), "LLM 返回格式解析失败，已忽略 LLM 结果。");
        }
    }

    private void sanitizeIssues(List<Issue> issues) {
        if (issues == null) {
            return;
        }
        for (Issue issue : issues) {
            if (issue.getSeverity() == null || issue.getSeverity().isBlank()) {
                issue.setSeverity("MEDIUM");
            }
            if (issue.getRule() == null || issue.getRule().isBlank()) {
                issue.setRule("LLM_REVIEW");
            }
            if (issue.getFilePath() == null || issue.getFilePath().isBlank()) {
                issue.setFilePath("UNKNOWN");
            }
            if (issue.getMessage() == null || issue.getMessage().isBlank()) {
                issue.setMessage("LLM 发现潜在问题。");
            }
            if (issue.getSuggestion() == null || issue.getSuggestion().isBlank()) {
                issue.setSuggestion("请结合上下文手动复核。");
            }
        }
    }

    private String buildPrompt(String diffContent) {
        return """
                你是资深代码审查工程师，请审查下面的 git diff，仅关注新增代码的风险。
                请返回严格 JSON，不要包含 markdown 代码块，不要输出额外文本。
                JSON 格式：
                {
                  "summary": "一句话总结",
                  "issues": [
                    {
                      "severity": "HIGH|MEDIUM|LOW",
                      "rule": "规则名",
                      "filePath": "文件路径",
                      "line": 123,
                      "message": "问题描述",
                      "suggestion": "修复建议"
                    }
                  ]
                }
                如果没有问题，issues 返回空数组。

                diff:
                """ + diffContent;
    }

    public record AgentReviewResult(List<Issue> issues, String summary) {
        public AgentReviewResult {
            if (issues == null) {
                issues = new ArrayList<>();
            }
            if (summary == null || summary.isBlank()) {
                summary = "LLM 审查完成。";
            }
        }
    }
}
