package com.buckle.inventory.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationException extends RuntimeException {

    private Map<String, String> fieldErrors;

    public ValidationException(String message) {
        super(message);
        this.fieldErrors = new HashMap<>();
    }

    public ValidationException(String field, String message) {
        super(message);
        this.fieldErrors = new HashMap<>();
        this.fieldErrors.put(field, message);
    }

    public void addFieldError(String field, String message) {
        if (this.fieldErrors == null) {
            this.fieldErrors = new HashMap<>();
        }
        this.fieldErrors.put(field, message);
    }
}
