package com.code.codewarden.controller;

import com.code.codewarden.dto.ReviewRequest;
import com.code.codewarden.dto.ReviewResponse;
import com.code.codewarden.service.GitDiffReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/review")
public class ReviewController {
    private final GitDiffReviewService gitDiffReviewService;

    public ReviewController(GitDiffReviewService gitDiffReviewService) {
        this.gitDiffReviewService = gitDiffReviewService;
    }

    @GetMapping("/health")
    public String health() {
        return "ok";
    }

    @PostMapping
    public ReviewResponse review(@Valid @RequestBody ReviewRequest request) {
        return gitDiffReviewService.review(request);
    }
}
