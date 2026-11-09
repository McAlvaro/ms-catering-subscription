package com.mcalvaro.mscatering.infrastructure.api;

import com.mcalvaro.mscatering.infrastructure.api.subscription.dto.ApiErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.beans.ConversionNotSupportedException;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should return 400 INVALID_PAYLOAD naming the parameter when a required parameter is missing")
    void shouldReturn400WhenRequiredParameterIsMissing() {
        // Arrange
        MissingServletRequestParameterException ex = new MissingServletRequestParameterException("reason", "String");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleMissingRequestParameter(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_PAYLOAD");
        assertThat(response.getBody().message()).contains("reason");
    }

    @Test
    @DisplayName("Should return 400 INVALID_PAYLOAD without echoing the rejected value when a LocalDate parameter is malformed")
    void shouldReturn400WithoutEchoingValueWhenDateParameterIsMalformed() throws Exception {
        // Arrange
        String rejectedValue = "secret-rejected-value";
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                rejectedValue, LocalDate.class, "date", dummyParameter(), new IllegalArgumentException("boom"));

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleArgumentTypeMismatch(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_PAYLOAD");
        assertThat(response.getBody().message()).contains("date").contains("yyyy-MM-dd")
                .doesNotContain(rejectedValue);
    }

    @Test
    @DisplayName("Should return 400 INVALID_PAYLOAD without date format hint when a non-date parameter is malformed")
    void shouldReturn400WithoutDateHintWhenNonDateParameterIsMalformed() throws Exception {
        // Arrange
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Integer.class, "count", dummyParameter(), new IllegalArgumentException("boom"));

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleArgumentTypeMismatch(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).contains("count").doesNotContain("yyyy-MM-dd").doesNotContain("abc");
    }

    @Test
    @DisplayName("Should keep returning 500 INTERNAL_SERVER_ERROR for ConversionNotSupportedException")
    void shouldReturn500ForConversionNotSupportedException() {
        // Arrange
        Exception ex = new ConversionNotSupportedException("value", LocalDate.class, new IllegalStateException("x"));

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    @Test
    @DisplayName("Should keep returning 500 INTERNAL_SERVER_ERROR for ServletRequestBindingException")
    void shouldReturn500ForServletRequestBindingException() {
        // Arrange
        Exception ex = new ServletRequestBindingException("binding failed");

        // Act
        ResponseEntity<ApiErrorResponse> response = handler.handleException(ex);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_SERVER_ERROR");
    }

    private static MethodParameter dummyParameter() throws NoSuchMethodException {
        return new MethodParameter(GlobalExceptionHandlerTest.class.getDeclaredMethod("dummyTarget", String.class), 0);
    }

    @SuppressWarnings("unused")
    private void dummyTarget(String value) {
    }
}
