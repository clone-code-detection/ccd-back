package github.clone_code_detection.exceptions.authen;

import github.clone_code_detection.exceptions.ExceptionBase;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class InvalidOperationException extends ExceptionBase {
    static final URI uri = URI.create("https://clone-code-detection.atlassian.net/wiki/spaces/CCD/pages/6914069/Authentication#Forbidden-request");

    public InvalidOperationException(String message) {
        this(initProblemDetail(message), null);
    }

    private InvalidOperationException(ProblemDetail detail, @Nullable Throwable cause) {
        super(HttpStatus.CONFLICT, detail, cause);
    }

    static ProblemDetail initProblemDetail(String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, message);
        detail.setType(uri);
        detail.setTitle("AUTHENTICATION INVALID OPERATION");
        return detail;
    }
}

