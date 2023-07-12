package github.clone_code_detection.exceptions;


import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

@Getter
public class ExceptionBase extends ErrorResponseException {
    public ExceptionBase(@NotNull HttpStatusCode statusCode, @NotNull ProblemDetail detail, @Nullable Throwable cause) {
        super(statusCode, detail, cause);
    }

    public ExceptionBase(@Nullable Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
