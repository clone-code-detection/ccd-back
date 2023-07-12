package github.clone_code_detection.controller.utils;

import github.clone_code_detection.exceptions.ExceptionBase;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@ControllerAdvice
@RestControllerAdvice
public class ExceptionBaseHandler {
    @ExceptionHandler(ExceptionBase.class)
    public ProblemDetail handleExceptionBase(ExceptionBase exception) {
        return exception.getBody();
    }
}
