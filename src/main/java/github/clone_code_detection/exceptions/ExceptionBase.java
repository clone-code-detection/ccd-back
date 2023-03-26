package github.clone_code_detection.exceptions;


import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.net.URI;

@Getter
public class ExceptionBase extends RuntimeException {
    private final URI uri;

    public ExceptionBase(@NotNull URI uri, @NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
        this.uri = uri;
    }

    public ExceptionBase(@NotNull URI uri, @NotNull String message) {
        super(message);
        this.uri = uri;
    }
}
