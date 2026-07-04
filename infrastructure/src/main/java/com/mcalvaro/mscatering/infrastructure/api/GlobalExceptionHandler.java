package com.mcalvaro.mscatering.infrastructure.api;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.infrastructure.api.subscription.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(DomainException ex) {
        ApiErrorResponse response = new ApiErrorResponse(ex.getCode(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String errorCode = "BAD_REQUEST";
        
        if (message != null && (message.toLowerCase().contains("not found") || message.toLowerCase().contains("no existe"))) {
            status = HttpStatus.NOT_FOUND;
            errorCode = "NOT_FOUND";
        }
        
        ApiErrorResponse response = new ApiErrorResponse(errorCode, message);
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex) {
        ApiErrorResponse response = new ApiErrorResponse("INTERNAL_SERVER_ERROR", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
