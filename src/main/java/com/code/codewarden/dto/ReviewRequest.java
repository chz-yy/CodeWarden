package com.code.codewarden.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotBlank(message = "diffContent 不能为空")
    private String diffContent; // 完整的diff字符串
    private String repoUrl;     // 仓库地址（可选，用于日志）
    private String branch;      // 分支名（可选）
}
