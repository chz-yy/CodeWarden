package com.code.codewarden.dto;

import lombok.Data;

import java.util.List;

@Data
public class ReviewResponse {
    private boolean success;
    private List<Issue> issues;
    private String summary;
}
