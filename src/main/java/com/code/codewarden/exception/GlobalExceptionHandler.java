package com.code.codewarden.exception;

import com.code.codewarden.dto.ReviewResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ReviewResponse handleValidation(MethodArgumentNotValidException ex) {
        ReviewResponse response = new ReviewResponse();
        response.setSuccess(false);
        response.setIssues(new ArrayList<>());
        response.setSummary("参数校验失败: " + ex.getBindingResult().getAllErrors().get(0).getDefaultMessage());
        return response;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ReviewResponse handleConstraint(ConstraintViolationException ex) {
        ReviewResponse response = new ReviewResponse();
        response.setSuccess(false);
        response.setIssues(new ArrayList<>());
        response.setSummary("参数校验失败: " + ex.getMessage());
        return response;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ReviewResponse handleGeneric(Exception ex) {
        ReviewResponse response = new ReviewResponse();
        response.setSuccess(false);
        response.setIssues(new ArrayList<>());
        response.setSummary("服务异常: " + ex.getMessage());
        return response;
    }
}
