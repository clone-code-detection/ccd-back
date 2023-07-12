package github.clone_code_detection.exceptions.moodle;

import github.clone_code_detection.exceptions.ExceptionBase;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class UnauthorizedException extends ExceptionBase {
    static final URI uri = URI.create("https://www.rfc-editor.org/rfc/rfc7807");

    public UnauthorizedException(String message) {
        this(initProblemDetail(message), null);
    }

    private UnauthorizedException(ProblemDetail detail, @Nullable Throwable cause) {
        super(HttpStatus.UNAUTHORIZED, detail, cause);
    }

    static ProblemDetail initProblemDetail(String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, message);
        detail.setType(uri);
        detail.setTitle("MOODLE UNAUTHORIZED");
        return detail;
    }
}
