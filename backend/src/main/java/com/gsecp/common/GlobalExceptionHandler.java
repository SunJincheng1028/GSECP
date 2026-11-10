package com.gsecp.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getCode()).body(R.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValid(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return badRequest(detail);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<Void>> handleBind(BindException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return badRequest(detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<R<Void>> handleConstraint(ConstraintViolationException e) {
        return badRequest(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<R<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(ResultCode.FORBIDDEN.code()).body(R.fail(ResultCode.FORBIDDEN));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<R<Void>> handleAuthentication(AuthenticationException e) {
        return ResponseEntity.status(ResultCode.UNAUTHORIZED.code()).body(R.fail(ResultCode.UNAUTHORIZED));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleUnexpected(Exception e) {
        log.error("未处理的异常", e);
        return ResponseEntity.status(ResultCode.INTERNAL_ERROR.code())
                .body(R.fail(ResultCode.INTERNAL_ERROR, "服务器内部错误，请联系管理员"));
    }

    private ResponseEntity<R<Void>> badRequest(String detail) {
        String message = (detail == null || detail.isBlank())
                ? ResultCode.BAD_REQUEST.message() : detail;
        return ResponseEntity.status(ResultCode.BAD_REQUEST.code())
                .body(R.fail(ResultCode.BAD_REQUEST.code(), message));
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
