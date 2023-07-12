package github.clone_code_detection.exceptions.moodle;

import github.clone_code_detection.exceptions.ExceptionBase;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class UnavailableException extends ExceptionBase {
    static final URI uri = URI.create("https://www.rfc-editor.org/rfc/rfc7807");

    public UnavailableException(String message) {
        this(initProblemDetail(message), null);
    }

    private UnavailableException(ProblemDetail detail, @Nullable Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, detail, cause);
    }

    static ProblemDetail initProblemDetail(String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, message);
        detail.setType(uri);
        detail.setTitle("MOODLE UNAVAILABLE");
        return detail;
    }
}
