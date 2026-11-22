package com.supportticket.exception;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.supportticket.dto.ApiError;
import com.supportticket.dto.FieldError;
import com.supportticket.model.TicketPriority;
import com.supportticket.model.TicketStatus;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.method.MethodValidationResult;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Turns every failure into the single {@link ApiError} shape with a plain-language message. */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    static final String TICKET_NOT_FOUND = "TICKET_NOT_FOUND";
    static final String INVALID_STATUS_TRANSITION = "INVALID_STATUS_TRANSITION";
    static final String CONCURRENT_MODIFICATION = "CONCURRENT_MODIFICATION";
    static final String REQUEST_ERROR = "REQUEST_ERROR";
    static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TicketNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(TicketNotFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, TICKET_NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of()));
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ApiError> handleInvalidTransition(
            InvalidStatusTransitionException ex, HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now(),
                409,
                INVALID_STATUS_TRANSITION,
                ex.getMessage(),
                request.getRequestURI(),
                List.of(),
                ex.getCurrentStatus(),
                ex.getRequestedStatus(),
                ex.getAllowedNextStatuses());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleConcurrentModification(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        log.debug("Concurrent modification: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(
                        409,
                        CONCURRENT_MODIFICATION,
                        "This ticket was changed by someone else. Reload it and try again.",
                        request.getRequestURI(),
                        List.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error handling {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(
                        500,
                        INTERNAL_ERROR,
                        "Something went wrong on our side. Please try again in a moment.",
                        request.getRequestURI(),
                        List.of()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldError(e.getField(), e.getDefaultMessage()))
                .toList();
        return validationFailure(errors, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return validationFailure(fieldErrorsOf(ex), request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(
            TypeMismatchException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        Class<?> required = ex.getRequiredType();
        String message = required != null && required.isEnum()
                ? unknownValueMessage(field, String.valueOf(ex.getValue()), required)
                : "'" + ex.getValue() + "' is not a valid value for " + field + ".";
        return validationFailure(List.of(new FieldError(field, message)), request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        if (ex.getCause() instanceof InvalidFormatException ife) {
            String field = ife.getPath().stream()
                    .map(ref -> ref.getFieldName())
                    .filter(name -> name != null)
                    .collect(Collectors.joining("."));
            Class<?> target = ife.getTargetType();
            String message = target != null && target.isEnum()
                    ? unknownValueMessage(field, String.valueOf(ife.getValue()), target)
                    : "'" + ife.getValue() + "' is not a valid value for " + field + ".";
            return validationFailure(List.of(new FieldError(field, message)), request);
        }
        return validationFailure(
                List.of(new FieldError("body", "The request body is missing or is not valid JSON.")), request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        int code = statusCode.value();
        String path = pathOf(request);
        ApiError error = code >= 500
                ? ApiError.of(code, INTERNAL_ERROR, "Something went wrong on our side. Please try again in a moment.", path, List.of())
                : ApiError.of(code, REQUEST_ERROR, requestErrorMessage(code), path, List.of());
        return ResponseEntity.status(statusCode).headers(headers).body(error);
    }

    private static ResponseEntity<Object> validationFailure(List<FieldError> errors, WebRequest request) {
        ApiError error = ApiError.of(400, VALIDATION_ERROR, "Request validation failed.", pathOf(request), errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    private static List<FieldError> fieldErrorsOf(MethodValidationResult result) {
        List<FieldError> errors = new ArrayList<>();
        for (ParameterValidationResult r : result.getParameterValidationResults()) {
            if (r instanceof ParameterErrors pe) {
                pe.getFieldErrors().forEach(e -> errors.add(new FieldError(e.getField(), e.getDefaultMessage())));
            } else {
                String name = r.getMethodParameter().getParameterName();
                for (MessageSourceResolvable e : r.getResolvableErrors()) {
                    errors.add(new FieldError(name, e.getDefaultMessage()));
                }
            }
        }
        return errors;
    }

    private static String unknownValueMessage(String field, String value, Class<?> enumType) {
        String valid = Arrays.stream(enumType.getEnumConstants())
                .map(c -> ((Enum<?>) c).name())
                .collect(Collectors.joining(", "));
        String label = enumType == TicketStatus.class
                ? "status"
                : enumType == TicketPriority.class ? "priority" : field;
        return "Unknown " + label + " '" + value + "'. Valid values: " + valid + ".";
    }

    private static String requestErrorMessage(int status) {
        return switch (status) {
            case 404 -> "The requested resource was not found.";
            case 405 -> "That operation is not supported for this resource.";
            case 415 -> "The request must be sent as JSON.";
            default -> "The request could not be processed.";
        };
    }

    private static String pathOf(WebRequest request) {
        return request instanceof ServletWebRequest swr ? swr.getRequest().getRequestURI() : "";
    }
}
