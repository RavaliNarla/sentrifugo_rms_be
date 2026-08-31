package com.bob.commonutil.exception;

import com.bob.commonutil.util.AppConstants;
import com.bob.db.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import javax.xml.bind.ValidationException;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --------------------- Validation Errors ---------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> errors = new LinkedHashMap<>(); // prevents duplicate key crash

        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                errors.put(fieldError.getField(), fieldError.getDefaultMessage())
        );

        logger.warn("Validation failed: {}", errors);

        ApiResponse<Map<String, String>> response =
                new ApiResponse<>(false, "Validation failed. Please check the errors.", errors);

        return ResponseEntity.badRequest().body(response);
    }

    // --------------------- HTTP Message Not Readable ---------------------
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidEnum(HttpMessageNotReadableException ex) {
        log.error("HTTP message not readable: {}", ex.getMessage(), ex);
        return ResponseEntity.badRequest().body(
                ApiResponse.error("Invalid request body")
        );
    }
    @ExceptionHandler({
            ClientAbortException.class,
            AsyncRequestNotUsableException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleClientDisconnect(Exception ex) {
        log.debug("Client disconnected: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.error("Client disconnected"));
    }

    // --------------------- Resource Not Found ---------------------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        logger.warn("Resource not found", ex);
        ApiResponse<Void> response = ApiResponse.error("Resource not found.");
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // --------------------- Duplicate Exception ---------------------
    @ExceptionHandler(DuplicateException.class)
    public ResponseEntity<ApiResponse<Void>> handleDuplicateException(DuplicateException ex, WebRequest request) {
        logger.warn("Resource not found", ex);
        ApiResponse<Void> response = ApiResponse.error(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // --------------------- Excel Validation ---------------------
    @ExceptionHandler(ExcelValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleExcelValidationException(ExcelValidationException ex) {
        logger.warn("Excel validation failed", ex);
        ApiResponse<Object> response = new ApiResponse<>(false, AppConstants.VALIDATION_FAILED_ERROR_MESSAGE, ex.getErrors());
        return new ResponseEntity<>(response, HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // --------------------- Illegal State ---------------------
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalStateException(IllegalStateException ex, WebRequest request) {
        logger.error("Illegal state exception", ex);
        ApiResponse<Object> response = new ApiResponse<>(false, AppConstants.UNEXPECTED_ERROR_MESSAGE, null);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    // --------------------- Illegal Argument ---------------------
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        logger.error("Illegal argument exception", ex);
        ApiResponse<Object> response = new ApiResponse<>(false, "Invalid request.", null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // --------------------- Invalid Password ---------------------
    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidPasswordException(InvalidPasswordException ex, WebRequest request) {
        logger.warn("Invalid password exception", ex);
        ApiResponse<Object> response = new ApiResponse<>(false, "Authentication failed.", null);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // --------------------- Authentication Failure ---------------------
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex, WebRequest request) {
        logger.warn("Authentication exception", ex);
        ApiResponse<Object> response = new ApiResponse<>(false, ex.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    // --------------------- Max Sessions Exceeded ---------------------
    @ExceptionHandler(MaxSessionsExceededException.class)
    public ResponseEntity<Map<String, Object>> handleMaxSessionsExceeded(MaxSessionsExceededException ex) {
        logger.warn("Max sessions exceeded for email: {}", ex.getEmail());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("error", "max_sessions_exceeded");
        response.put("error_description", ex.getMessage());
        response.put("max_sessions", ex.getMaxSessions());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // --------------------- Invalid File Type ---------------------
    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidFileTypeException(InvalidFileTypeException ex, WebRequest request) {
        logger.warn("Invalid file type uploaded: {}", ex.getMessage());
        ApiResponse<Object> response = new ApiResponse<>(false, ex.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    // --------------------- ValidationException ---------------------
    @ExceptionHandler(ManualValidationException.class)
    public ResponseEntity<ApiResponse<Object>> handleExcelValidationException(ManualValidationException ex,WebRequest request) {
        logger.warn(AppConstants.VALIDATION_FAILED_ERROR_MESSAGE, ex);
        ApiResponse<Object> response = new ApiResponse<>(false, AppConstants.VALIDATION_FAILED_ERROR_MESSAGE, ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }


    // --------------------- Business / Runtime Exceptions ---------------------
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(RuntimeException ex, WebRequest request) {
        logger.error("Unhandled runtime exception", ex);
        ApiResponse<Object> response = new ApiResponse<>(false, AppConstants.UNEXPECTED_ERROR_MESSAGE, null);
        return ResponseEntity.badRequest().body(response);
    }

    // --------------------- Global Catch-All ---------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleAllExceptions(Exception ex, WebRequest request) {
        logger.error("Unhandled exception occurred: {}", ex.getMessage(), ex);
        ApiResponse<Object> response =
                new ApiResponse<>(false, AppConstants.UNEXPECTED_ERROR_MESSAGE, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // --------------------- Global Catch-All ---------------------
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(AccessDeniedException ex, WebRequest request) {
        logger.error("Access denied exception", ex);
        ApiResponse<Object> response = new ApiResponse<>(false, "Access denied.", null);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ApiResponse<Object>> handleCommonException(CommonException ex, WebRequest request) {
        logger.error("Exception", ex);
        ApiResponse<Object> response = new ApiResponse<>(false, ex.getMessage(), null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SchedulingConflictException.class)
    public ResponseEntity<ApiResponse<Object>> handleScheduleConflictValidation(SchedulingConflictException ex, WebRequest request) {
        logger.error("Exception", ex);
        ApiResponse<Object> response = new ApiResponse<>(false, ex.getMessage(), ex.getResponse());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
