package github.clone_code_detection.exceptions.report;

import jakarta.validation.constraints.NotNull;

import java.util.Arrays;
import java.util.List;

public class HighlightSessionException extends RuntimeException {
    private final Throwable cause;

    public HighlightSessionException(@NotNull Throwable cause) {
        super("", cause);
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
