package com.mcalvaro.mscatering.infrastructure.api;

import com.mcalvaro.mscatering.domain.core.DomainException;
import com.mcalvaro.mscatering.infrastructure.api.subscription.dto.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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

        if (message != null
                && (message.toLowerCase().contains("not found") || message.toLowerCase().contains("no existe"))) {
            status = HttpStatus.NOT_FOUND;
            errorCode = "NOT_FOUND";
        }

        ApiErrorResponse response = new ApiErrorResponse(errorCode, message);
        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> manejarErroresDeValidacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errores.put(error.getField(), error.getDefaultMessage());
        });
        
        return ResponseEntity.badRequest().body(errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String message = "El contenido de la petición es inválido o no se pudo leer correctamente.";
        String fieldName = null;

        Throwable cause = ex.getCause();
        if (cause != null) {
            String causeClassName = cause.getClass().getName();
            if (causeClassName.endsWith(".InvalidFormatException") || causeClassName.endsWith(".MismatchedInputException")) {
                try {
                    // Utilizar reflexión para obtener el targetType
                    java.lang.reflect.Method getTargetTypeMethod = cause.getClass().getMethod("getTargetType");
                    Class<?> targetType = (Class<?>) getTargetTypeMethod.invoke(cause);
                    
                    // Utilizar reflexión para obtener el path
                    java.lang.reflect.Method getPathMethod = cause.getClass().getMethod("getPath");
                    java.util.List<?> path = (java.util.List<?>) getPathMethod.invoke(cause);
                    
                    if (path != null && !path.isEmpty()) {
                        Object lastRef = path.get(path.size() - 1);
                        String fName = null;
                        try {
                            java.lang.reflect.Method getFieldNameMethod = lastRef.getClass().getMethod("getFieldName");
                            fName = (String) getFieldNameMethod.invoke(lastRef);
                        } catch (NoSuchMethodException e) {
                            try {
                                java.lang.reflect.Method getNameMethod = lastRef.getClass().getMethod("getName");
                                fName = (String) getNameMethod.invoke(lastRef);
                            } catch (NoSuchMethodException e2) {
                            }
                        }
                        if (fName != null) {
                            fieldName = fName;
                        }
                    }
                    
                    if (targetType != null && targetType.isEnum()) {
                        String enumValues = Arrays.toString(targetType.getEnumConstants());
                        Object rejectedValue = "inválido";
                        
                        try {
                            java.lang.reflect.Method getValueMethod = cause.getClass().getMethod("getValue");
                            rejectedValue = getValueMethod.invoke(cause);
                        } catch (Exception ignored) {}
                        
                        message = String.format("El valor '%s' no es válido para el campo '%s'. Los valores permitidos son: %s",
                                rejectedValue, fieldName != null ? fieldName : "desconocido", enumValues);
                    } else if (fieldName != null) {
                        message = String.format("El formato del campo '%s' es inválido.", fieldName);
                    }
                } catch (Exception ignored) {
                }
            }
        }

        ApiErrorResponse response = new ApiErrorResponse("INVALID_PAYLOAD", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleException(Exception ex) {
        ex.printStackTrace();
        ApiErrorResponse response = new ApiErrorResponse("INTERNAL_SERVER_ERROR",
                "Ocurrió un error inesperado en el servidor. Por favor, intente más tarde.");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
