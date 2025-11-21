package com.elina.authorization.controller;

import com.elina.authorization.exception.AuthenticationException;
import com.elina.authorization.rule.BusinessRuleException;
import com.elina.projects.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for consistent error responses.
 * 
 * Handles:
 * - BusinessRuleException → HTTP 400 (BAD_REQUEST)
 * - AuthenticationException → HTTP 400 (BAD_REQUEST)
 * - NotFoundException → HTTP 404 (NOT_FOUND)
 * - AccessDeniedException → HTTP 403 (FORBIDDEN)
 * - Validation errors → HTTP 400 (BAD_REQUEST)
 * - General RuntimeExceptions → HTTP 500 (INTERNAL_SERVER_ERROR)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessRuleException(BusinessRuleException e) {
        logger.warn("BusinessRuleException [Rule {}]: {}", e.getRuleNumber(), e.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("message", e.getMessage());
        error.put("ruleNumber", e.getRuleNumber());
        if (e.getHint() != null) {
            error.put("hint", e.getHint());
        }
        error.put("type", "BUSINESS_RULE_VIOLATION");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationException(AuthenticationException e) {
        logger.warn("AuthenticationException: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage());
        error.put("type", "AUTHENTICATION_ERROR");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFoundException(NotFoundException e) {
        logger.info("NotFoundException: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage());
        error.put("type", "NOT_FOUND");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDeniedException(AccessDeniedException e) {
        logger.warn("AccessDeniedException: {}", e.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("message", "Access denied. You do not have permission to perform this operation.");
        error.put("type", "ACCESS_DENIED");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException e) {
        logger.warn("RuntimeException: {}", e.getMessage(), e);
        Map<String, String> error = new HashMap<>();
        error.put("message", e.getMessage());
        error.put("type", "RUNTIME_ERROR");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        logger.warn("Validation errors: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }
}

