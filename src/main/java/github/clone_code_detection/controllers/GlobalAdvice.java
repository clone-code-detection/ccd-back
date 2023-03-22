package github.clone_code_detection.controllers;

import github.clone_code_detection.entity.ResponseUnified;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
@ControllerAdvice
class GlobalAdvice extends ResponseEntityExceptionHandler {
    @ExceptionHandler(value = {AuthenticationException.class})
    public ResponseEntity<Object> handleAuthentication(RuntimeException ex, WebRequest request) {
        ResponseUnified<String> response = ResponseUnified.<String>builder()
                                                          .code(-1)
                                                          .data(ex.getMessage())
                                                          .build();
        return handleExceptionInternal(ex, response, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(value = {ConstraintViolationException.class, ValidationException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseUnified<String> handleConstraints(RuntimeException ex) {
        return ResponseUnified.<String>builder()
                              .code(-2)
                              .data(ex.getMessage())
                              .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseUnified<String> handleUnwantedException(Exception ex) {
        return ResponseUnified.<String>builder()
                              .code(-3)
                              .data(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                              .build();
    }
}
