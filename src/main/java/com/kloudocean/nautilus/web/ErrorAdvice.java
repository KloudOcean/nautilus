package com.kloudocean.nautilus.web;

import com.kloudocean.nautilus.provider.ProviderException;
import com.kloudocean.nautilus.routing.NoEligibleProviderException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps internal exceptions to HTTP responses with stable error shapes.
 * Response shape mirrors OpenAI's error envelope so existing clients can parse it.
 */
@RestControllerAdvice
public class ErrorAdvice {

    private static final Logger log = LoggerFactory.getLogger(ErrorAdvice.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> onValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst()
                .orElse("invalid request");
        return error(HttpStatus.BAD_REQUEST, "invalid_request_error", message, null);
    }

    @ExceptionHandler(NoEligibleProviderException.class)
    public ResponseEntity<Map<String, Object>> onNoProvider(NoEligibleProviderException e) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "no_eligible_provider", e.getMessage(), null);
    }

    @ExceptionHandler(ProviderException.class)
    public ResponseEntity<Map<String, Object>> onProvider(ProviderException e) {
        HttpStatus status = switch (e.kind()) {
            case RATE_LIMIT -> HttpStatus.TOO_MANY_REQUESTS;
            case TIMEOUT, UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case AUTH -> HttpStatus.UNAUTHORIZED;
            case BAD_REQUEST -> HttpStatus.BAD_REQUEST;
            case UPSTREAM_ERROR -> HttpStatus.BAD_GATEWAY;
        };
        return error(status, e.kind().name().toLowerCase(), e.getMessage(), e.providerName());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> onIllegalArgument(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, "invalid_request_error", e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> onUnknown(Exception e) {
        log.error("unhandled exception", e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error",
                "An unexpected error occurred. See server logs.", null);
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String type,
                                                       String message, String provider) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("type", type);
        err.put("message", message);
        if (provider != null) {
            err.put("provider", provider);
        }
        err.put("status", status.value());
        err.put("timestamp", Instant.now().toString());

        Map<String, Object> body = Map.of("error", err);
        return ResponseEntity.status(status).body(body);
    }
}
