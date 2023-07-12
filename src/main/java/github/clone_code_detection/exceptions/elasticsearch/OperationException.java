package github.clone_code_detection.exceptions.elasticsearch;

import github.clone_code_detection.exceptions.ExceptionBase;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class OperationException extends ExceptionBase {
    static final URI uri = URI.create("https://clone-code-detection.atlassian.net/wiki/spaces/CCD/pages/6914069/Authentication#Bad-request");

    public OperationException(String message) {
        this(initProblemDetail(message), null);
    }

    public OperationException(String message, Throwable cause) {
        this(initProblemDetail(message), cause);
    }

    private OperationException(ProblemDetail detail, @Nullable Throwable cause) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, detail, cause);
    }

    static ProblemDetail initProblemDetail(String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, message);
        detail.setType(uri);
        detail.setTitle("ELASTICSEARCH CRUD");
        return detail;
    }
}
