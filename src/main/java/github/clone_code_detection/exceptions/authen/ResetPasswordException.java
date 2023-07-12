package github.clone_code_detection.exceptions.authen;


import github.clone_code_detection.exceptions.ExceptionBase;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class ResetPasswordException extends ExceptionBase {
    static final URI uri = URI.create(
            "https://clone-code-detection.atlassian.net/wiki/spaces/CCD/pages/6914069/Authentication#Bad-request");

    public ResetPasswordException(String message) {
        this(initProblemDetail(message), null);
    }

    private ResetPasswordException(ProblemDetail detail, @Nullable Throwable cause) {
        super(HttpStatus.NOT_ACCEPTABLE, detail, cause);
    }

    static ProblemDetail initProblemDetail(String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_ACCEPTABLE, message);
        detail.setType(uri);
        detail.setTitle("ACTIVATE ACCOUNT FAILED");
        return detail;
    }
}