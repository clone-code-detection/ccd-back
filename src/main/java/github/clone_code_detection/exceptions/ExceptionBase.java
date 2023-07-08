package github.clone_code_detection.exceptions;


import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ExceptionBase extends RuntimeException {
    public ExceptionBase(@NotNull String message, @Nullable Throwable cause) {
        super(message, cause);
    }

    public ExceptionBase(@NotNull String message) {
        super(message);
    }
}
