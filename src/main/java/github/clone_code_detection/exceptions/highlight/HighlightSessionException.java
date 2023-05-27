package github.clone_code_detection.exceptions.highlight;

import github.clone_code_detection.exceptions.ExceptionBase;

import javax.annotation.Nullable;
import java.net.URI;

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
        if (cause != null) {
            exception = exception.concat(cause.getMessage() + "\n");
            for (StackTraceElement stackTraceElement : cause.getStackTrace()) {
                exception = exception.concat(String.format("%s - %s: %s.\n", stackTraceElement.getFileName(), stackTraceElement.getClassName(), stackTraceElement.getMethodName()));
            }
        }
        return exception;
    }
}
