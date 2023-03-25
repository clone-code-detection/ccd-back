package github.clone_code_detection.controllers;

import github.clone_code_detection.entity.ResponseUnified;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.text.MessageFormat;


@RestControllerAdvice
@Slf4j
class GlobalAdvice {
    @ExceptionHandler(value = {AuthenticationException.class})
    public ResponseUnified<String> handleAuthentication(RuntimeException ex) {
        return ResponseUnified.<String>builder()
                              .code(-1)
                              .data(ex.getMessage())
                              .build();
    }

    @ExceptionHandler(value = {ConstraintViolationException.class, ValidationException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseUnified<String> handleConstraints(Exception ex) {
        return ResponseUnified.<String>builder()
                              .code(-2)
                              .data(ex.getMessage())
                              .build();
    }

    @ExceptionHandler(value = {BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseUnified<String> handleConstraints(BindException ex) {
        ObjectError message = ex.getAllErrors()
                                .get(0);

        return ResponseUnified.<String>builder()
                              .code(-2)
                              .data(MessageFormat.format("{0} : {1}",
                                                         message.getObjectName(),
                                                         message.getDefaultMessage()))
                              .build();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseUnified<String> handleUnwantedException(Exception ex) {
        log.error("Internal error", ex);
        return ResponseUnified.<String>builder()
                              .code(-3)
                              .data(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                              .build();
    }
}
