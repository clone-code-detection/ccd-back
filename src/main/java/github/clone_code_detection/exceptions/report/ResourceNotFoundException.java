package github.clone_code_detection.exceptions.report;

import github.clone_code_detection.exceptions.ExceptionBase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class ResourceNotFoundException extends ExceptionBase {
    static final URI uri = URI.create("https://www.rfc-editor.org/rfc/rfc7807");

    public ResourceNotFoundException(String message) {
        this(initProblemDetail(message));
    }

    private ResourceNotFoundException(ProblemDetail detail) {
        super(HttpStatus.NOT_FOUND, detail, null);
    }

    static ProblemDetail initProblemDetail(String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, message);
        detail.setType(uri);
        detail.setTitle("NOT FOUND");
        return detail;
    }
}
