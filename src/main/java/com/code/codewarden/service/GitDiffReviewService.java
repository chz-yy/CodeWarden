package com.code.codewarden.service;

import com.code.codewarden.dto.Issue;
import com.code.codewarden.dto.ReviewRequest;
import com.code.codewarden.dto.ReviewResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GitDiffReviewService {
    private static final Pattern HUNK_PATTERN = Pattern.compile("@@ -\\d+(?:,\\d+)? \\+(\\d+)(?:,\\d+)? @@.*");
    private static final Pattern SECRET_PATTERN = Pattern.compile(
            "(?i)(password|passwd|secret|token|api[-_]?key)\\s*[:=]\\s*[\"'][^\"']+[\"']"
    );
    private final CodeReviewAgentService codeReviewAgentService;

    public GitDiffReviewService(CodeReviewAgentService codeReviewAgentService) {
        this.codeReviewAgentService = codeReviewAgentService;
    }

    public ReviewResponse review(ReviewRequest request) {
        List<Issue> issues = new ArrayList<>();
        List<DiffLine> diffLines = parseAddedLines(request.getDiffContent());

        for (DiffLine line : diffLines) {
            checkTodoFixme(line, issues);
            checkDebugOutput(line, issues);
            checkPotentialSecret(line, issues);
            checkLongLine(line, issues);
        }

        CodeReviewAgentService.AgentReviewResult agentReviewResult =
                codeReviewAgentService.reviewDiff(request.getDiffContent());
        issues.addAll(agentReviewResult.issues());

        ReviewResponse response = new ReviewResponse();
        response.setSuccess(true);
        response.setIssues(issues);
        response.setSummary(buildSummary(issues, request, agentReviewResult.summary()));
        return response;
    }

    private String buildSummary(List<Issue> issues, ReviewRequest request, String llmSummary) {
        if (issues.isEmpty()) {
            return String.format("审查完成：未发现明显问题。branch=%s, repo=%s。LLM: %s",
                    safeValue(request.getBranch()), safeValue(request.getRepoUrl()), safeValue(llmSummary));
        }
        return String.format("审查完成：发现 %d 个问题。branch=%s, repo=%s。LLM: %s",
                issues.size(), safeValue(request.getBranch()), safeValue(request.getRepoUrl()), safeValue(llmSummary));
    }

    private String safeValue(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private void checkTodoFixme(DiffLine line, List<Issue> issues) {
        String textUpper = line.content.toUpperCase();
        if (textUpper.contains("TODO") || textUpper.contains("FIXME")) {
            issues.add(new Issue(
                    "LOW",
                    "COMMENT_TODO_FIXME",
                    line.filePath,
                    line.lineNumber,
                    "新增代码中包含 TODO/FIXME，可能存在未完成逻辑。",
                    "确认该注释是否允许进入主干，必要时补齐实现。"
            ));
        }
    }

    private void checkDebugOutput(DiffLine line, List<Issue> issues) {
        String trimmed = line.content.trim();
        if (trimmed.contains("System.out.print")
                || trimmed.contains("printStackTrace(")
                || trimmed.contains("console.log(")) {
            issues.add(new Issue(
                    "MEDIUM",
                    "DEBUG_OUTPUT",
                    line.filePath,
                    line.lineNumber,
                    "检测到调试输出语句，可能污染生产日志。",
                    "使用正式日志框架（如 slf4j）并控制日志级别。"
            ));
        }
    }

    private void checkPotentialSecret(DiffLine line, List<Issue> issues) {
        Matcher matcher = SECRET_PATTERN.matcher(line.content);
        if (matcher.find()) {
            issues.add(new Issue(
                    "HIGH",
                    "POTENTIAL_SECRET",
                    line.filePath,
                    line.lineNumber,
                    "疑似硬编码敏感信息（密码/密钥/token）。",
                    "改为环境变量或配置中心注入，避免明文提交。"
            ));
        }
    }

    private void checkLongLine(DiffLine line, List<Issue> issues) {
        if (line.content.length() > 140) {
            issues.add(new Issue(
                    "LOW",
                    "LINE_TOO_LONG",
                    line.filePath,
                    line.lineNumber,
                    "单行代码长度超过 140，影响可读性。",
                    "考虑拆分表达式或提取变量。"
            ));
        }
    }

    private List<DiffLine> parseAddedLines(String diffContent) {
        List<DiffLine> result = new ArrayList<>();
        if (diffContent == null || diffContent.isBlank()) {
            return result;
        }

        String currentFile = "UNKNOWN";
        int newLinePointer = -1;
        String[] lines = diffContent.split("\\R");

        for (String rawLine : lines) {
            if (rawLine.startsWith("+++ ")) {
                currentFile = normalizeFilePath(rawLine.substring(4).trim());
                continue;
            }

            Matcher hunkMatcher = HUNK_PATTERN.matcher(rawLine);
            if (hunkMatcher.matches()) {
                newLinePointer = Integer.parseInt(hunkMatcher.group(1));
                continue;
            }

            if (rawLine.startsWith("+") && !rawLine.startsWith("+++")) {
                if (newLinePointer >= 0) {
                    result.add(new DiffLine(currentFile, newLinePointer, rawLine.substring(1)));
                    newLinePointer++;
                }
                continue;
            }

            if (rawLine.startsWith(" ")) {
                if (newLinePointer >= 0) {
                    newLinePointer++;
                }
                continue;
            }

            // 删除行或其他元数据不增加 new file 行号
        }

        return result;
    }

    private String normalizeFilePath(String rawPath) {
        if (rawPath.startsWith("a/") || rawPath.startsWith("b/")) {
            return rawPath.substring(2);
        }
        return rawPath;
    }

    private static class DiffLine {
        private final String filePath;
        private final int lineNumber;
        private final String content;

        private DiffLine(String filePath, int lineNumber, String content) {
            this.filePath = filePath;
            this.lineNumber = lineNumber;
            this.content = content;
        }
    }
}
