package com.code.codewarden.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Issue {
    private String severity;
    private String rule;
    private String filePath;
    private Integer line;
    private String message;
    private String suggestion;
}
