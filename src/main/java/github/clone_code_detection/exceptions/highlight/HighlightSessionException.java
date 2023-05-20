package github.clone_code_detection.exceptions.highlight;

import github.clone_code_detection.exceptions.ExceptionBase;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Arrays;

public class HighlightSessionException extends ExceptionBase {
    private static final URI uri = URI.create("https://clone-code-detection.atlassian.net/wiki/spaces/CCD/pages/9601025/Highlight");
    private final Throwable cause;
    private final String message;

    public HighlightSessionException(String message, @Nullable Throwable cause) {
        super(uri, message, cause);
        this.message = message;
        this.cause = cause;
    }

    @Override
    public String toString() {
        String exception = message + "\n";
        assert cause != null;
        for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
            exception = exception.concat(stackTraceElement.toString() + "\n");
        }
        System.out.println("cause: " + cause.getCause());
        System.out.println("suppressed: " + Arrays.toString(cause.getSuppressed()));
        return exception;
    }
}
