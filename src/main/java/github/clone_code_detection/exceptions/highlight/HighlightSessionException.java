package github.clone_code_detection.exceptions.highlight;

import github.clone_code_detection.exceptions.ExceptionBase;
import jakarta.validation.constraints.NotNull;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

public class HighlightSessionException extends ExceptionBase {
    private static final URI uri = URI.create(
            "https://clone-code-detection.atlassian.net/wiki/spaces/CCD/pages/9601025/Highlight");
    private final Throwable cause;

    public HighlightSessionException(@NotNull Throwable cause) {
        super(uri, "", cause);
        this.cause = cause;
    }

    @Override
    public String toString() {
        List<StackTraceElement> elements = Arrays.stream(cause.getStackTrace()).toList();
        final String[] exception = {""};
        elements.forEach(element -> exception[0] = String.join("\n", exception[0], String.valueOf(element)));
        return exception[0];
    }
}
