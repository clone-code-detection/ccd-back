package github.clone_code_detection.exceptions.authen;

import github.clone_code_detection.exceptions.ExceptionBase;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class FieldValidationException extends ExceptionBase {
    static final String base_uri = "https://clone-code-detection.atlassian.net/wiki/spaces/CCD/pages/6914069/Authentication#Bad-request-";

    public FieldValidationException(String field, String message) {
        this(initProblemDetail(message, field), null);
    }

    private FieldValidationException(ProblemDetail detail, @Nullable Throwable cause) {
        super(HttpStatus.BAD_REQUEST, detail, cause);
    }

    static ProblemDetail initProblemDetail(String message, String field) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setType(URI.create(base_uri + field));
        detail.setTitle("FIELD VALIDATION FAILED");
        return detail;
    }
}
