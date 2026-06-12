package com.certifreight.backend.exception;

import com.certifreight.backend.model.ShipmentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.WebRequest;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldBuildConstraintViolationResponseFromFieldValidationMessage() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ShipmentRequest(), "shipmentRequest");
        bindingResult.addError(new FieldError("shipmentRequest", "trackingNumber", "tracking invalid"));

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter(), bindingResult);

        ResponseEntity<Object> response = Objects.requireNonNull(handler.handleMethodArgumentNotValid(
                ex,
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                mock(WebRequest.class)
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ProblemDetail body = (ProblemDetail) Objects.requireNonNull(response.getBody());
        assertEquals("Constraint Violation", body.getTitle());
        assertEquals("tracking invalid", body.getDetail());
    }

    @Test
    void shouldUseFallbackValidationMessageWhenNoFieldErrorsExist() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new ShipmentRequest(), "shipmentRequest");
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(methodParameter(), bindingResult);

        ResponseEntity<Object> response = Objects.requireNonNull(handler.handleMethodArgumentNotValid(
                ex,
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                mock(WebRequest.class)
        ));

        ProblemDetail body = (ProblemDetail) Objects.requireNonNull(response.getBody());
        assertEquals("Invalid payload parameters provided", body.getDetail());
    }

    @Test
    void shouldBuildMalformedPayloadResponseForUnreadableJson() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "bad payload",
                new IllegalArgumentException("invalid numeric value"),
                mock(HttpInputMessage.class)
        );

        ResponseEntity<Object> response = Objects.requireNonNull(handler.handleHttpMessageNotReadable(
                ex,
                new HttpHeaders(),
                HttpStatus.BAD_REQUEST,
                mock(WebRequest.class)
        ));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ProblemDetail body = (ProblemDetail) Objects.requireNonNull(response.getBody());
        assertEquals("Malformed Message Payload", body.getTitle());
        String detail = Objects.requireNonNull(body.getDetail());
        assertTrue(detail.contains("invalid numeric value"));
    }

    @Test
    void shouldReturnForbiddenForAccessDeniedWithMessage() {
        ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(new AccessDeniedException("forbidden path"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Forbidden", response.getBody().getTitle());
        assertEquals("forbidden path", response.getBody().getDetail());
    }

    @Test
    void shouldReturnForbiddenForAccessDeniedWithoutMessage() {
        AccessDeniedException accessDenied = mock(AccessDeniedException.class);
        when(accessDenied.getMessage()).thenReturn(null);

        ResponseEntity<ProblemDetail> response = handler.handleAccessDenied(accessDenied);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Insufficient permissions for requested resource", response.getBody().getDetail());
    }

    @Test
    void shouldReturnInternalServerErrorForGenericExceptionWithMessage() {
        ResponseEntity<ProblemDetail> response = handler.handleGeneralServerFailure(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal Server Error", response.getBody().getTitle());
        assertEquals("boom", response.getBody().getDetail());
    }

    @Test
    void shouldReturnInternalServerErrorWithFallbackMessageWhenExceptionMessageIsNull() {
        ResponseEntity<ProblemDetail> response = handler.handleGeneralServerFailure(new Exception((String) null));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("An unclassified execution error occurred", response.getBody().getDetail());
    }

    private MethodParameter methodParameter() throws NoSuchMethodException {
        return new MethodParameter(DummyEndpoint.class.getDeclaredMethod("create", ShipmentRequest.class), 0);
    }

    private static class DummyEndpoint {
        @SuppressWarnings("unused")
        public void create(ShipmentRequest request) {
            // method exists only to provide MethodParameter for MethodArgumentNotValidException
        }
    }
}

