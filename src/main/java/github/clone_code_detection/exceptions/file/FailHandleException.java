package github.clone_code_detection.exceptions.file;

import github.clone_code_detection.exceptions.ExceptionBase;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class FailHandleException extends ExceptionBase {
    static final URI uri = URI.create("https://www.rfc-editor.org/rfc/rfc7807");

    public FailHandleException(String message) {
        this(initProblemDetail(message), null);
    }

    private FailHandleException(ProblemDetail detail, @Nullable Throwable cause) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, detail, cause);
    }

    static ProblemDetail initProblemDetail(String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, message);
        detail.setType(uri);
        detail.setTitle("FILE HANDLE FAILED");
        return detail;
    }
}
