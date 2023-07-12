package github.clone_code_detection.exceptions.file;

import github.clone_code_detection.exceptions.ExceptionBase;
import jakarta.annotation.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

public class UnsupportedLanguageException extends ExceptionBase {
    static final URI uri = URI.create("https://www.rfc-editor.org/rfc/rfc7807");

    public UnsupportedLanguageException(String message) {
        this(initProblemDetail(message), null);
    }

    private UnsupportedLanguageException(ProblemDetail detail, @Nullable Throwable cause) {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE, detail, cause);
    }

    static ProblemDetail initProblemDetail(String message) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message);
        detail.setType(uri);
        detail.setTitle("UNSUPPROTED FILE LANGUAGE");
        return detail;
    }
}
