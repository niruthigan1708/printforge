package com.printforge.exception;

import com.printforge.dto.ErrorResponse;
import com.printforge.security.LoginAttemptService;
import org.springframework.dao.DataIntegrityViolationException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex) {
    HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
    if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
    return ResponseEntity.status(status).body(new ErrorResponse(ex.getReason(), status.value()));
  }

  @ExceptionHandler(LoginAttemptService.TooManyAttemptsException.class)
  public ResponseEntity<ErrorResponse> handleTooManyAttempts(LoginAttemptService.TooManyAttemptsException ex) {
    return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
      .header("Retry-After", Long.toString(ex.retryAfterSeconds()))
      .body(new ErrorResponse(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS.value()));
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(EntityNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND.value()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage()));
    // The UI shows `message`, so spell out which fields failed rather than just "Validation failed".
    String detail = errors.entrySet().stream().sorted(Map.Entry.comparingByKey())
      .map(entry -> entry.getKey() + " " + entry.getValue()).collect(java.util.stream.Collectors.joining("; "));
    String message = detail.isEmpty() ? "Validation failed" : "Validation failed: " + detail;
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(message, HttpStatus.BAD_REQUEST.value(), errors));
  }

  /**
   * Safety net for anything the database itself rejects (a value too long for its column, a
   * unique or foreign-key constraint) that validation did not catch: the request conflicts
   * with what can be stored, which is not a server crash.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
    log.warn("Database rejected a write: {}", ex.getMostSpecificCause().getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("The request could not be saved because a value is invalid or conflicts with existing data", HttpStatus.CONFLICT.value()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST.value()));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ErrorResponse> handleMaxUpload(MaxUploadSizeExceededException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("File exceeds the maximum allowed size of 20MB", HttpStatus.BAD_REQUEST.value()));
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse("You do not have permission to perform this action", HttpStatus.FORBIDDEN.value()));
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("Authentication is required", HttpStatus.UNAUTHORIZED.value()));
  }

  /** Malformed JSON, or a value that cannot be converted (e.g. an unknown status enum). */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Request body is malformed or contains an invalid value", HttpStatus.BAD_REQUEST.value()));
  }

  /** A path or query parameter of the wrong type, e.g. /api/products/abc. */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("Invalid value for parameter '" + ex.getName() + "'", HttpStatus.BAD_REQUEST.value()));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("The requested resource was not found", HttpStatus.NOT_FOUND.value()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
    // Spring MVC's own exceptions (missing parameter/part, unsupported method or media type, ...)
    // carry the right 4xx status; don't flatten them into a 500.
    if (ex instanceof org.springframework.web.ErrorResponse springError && !springError.getStatusCode().is5xxServerError()) {
      int status = springError.getStatusCode().value();
      String detail = springError.getBody().getDetail();
      return ResponseEntity.status(status).body(new ErrorResponse(detail != null ? detail : "The request could not be processed", status));
    }
    log.error("Unhandled exception", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse("An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value()));
  }
}
