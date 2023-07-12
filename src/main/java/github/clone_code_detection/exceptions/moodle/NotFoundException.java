package github.clone_code_detection.exceptions.moodle;

import github.clone_code_detection.exceptions.ExceptionBase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class NotFoundException extends ExceptionBase {
    static final URI uri = URI.create("https://www.rfc-editor.org/rfc/rfc7807");

    public NotFoundException(String message) {
        this(initProblemDetail(message));
    }

    private NotFoundException(ProblemDetail detail) {
        super(HttpStatus.NOT_FOUND, detail, null);
    }

    static ProblemDetail initProblemDetail(String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, message);
        detail.setType(uri);
        detail.setTitle("MOODLE NOT FOUND");
        return detail;
    }
}
