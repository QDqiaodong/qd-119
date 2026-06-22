package com.buckle.inventory.exception;

import com.buckle.inventory.dto.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public Result<Map<String, Object>> handleValidationException(ValidationException e) {
        Map<String, Object> data = new HashMap<>();
        if (e.getFieldErrors() != null && !e.getFieldErrors().isEmpty()) {
            data.put("fieldErrors", e.getFieldErrors());
        }
        String message = e.getMessage() != null ? e.getMessage() : "参数校验失败";
        return Result.error(400, message, data);
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<Object> handleRuntimeException(RuntimeException e) {
        return Result.error(500, e.getMessage() != null ? e.getMessage() : "服务器内部错误", null);
    }

    @ExceptionHandler(Exception.class)
    public Result<Object> handleException(Exception e) {
        return Result.error(500, "服务器内部错误", null);
    }
}
