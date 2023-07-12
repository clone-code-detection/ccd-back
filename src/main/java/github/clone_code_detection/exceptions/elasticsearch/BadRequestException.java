package github.clone_code_detection.exceptions.elasticsearch;

import github.clone_code_detection.exceptions.ExceptionBase;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class BadRequestException extends ExceptionBase {
    static final URI uri = URI.create("https://clone-code-detection.atlassian.net/wiki/spaces/CCD/pages/6914069/Authentication#Bad-request");

    public BadRequestException(String message) {
        this(initProblemDetail(message), null);
    }

    private BadRequestException(ProblemDetail detail, @Nullable Throwable cause) {
        super(HttpStatus.BAD_REQUEST, detail, cause);
    }

    static ProblemDetail initProblemDetail(String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, message);
        detail.setType(uri);
        detail.setTitle("ELASTICSEARCH BUILD");
        return detail;
    }
}
