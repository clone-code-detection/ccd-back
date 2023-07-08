package github.clone_code_detection.exceptions.moodle;

import github.clone_code_detection.exceptions.ExceptionBase;
import jakarta.validation.constraints.NotNull;

public class MoodleAuthenticationException extends ExceptionBase {
    public MoodleAuthenticationException(@NotNull String message) {
        super(message);
    }
}
