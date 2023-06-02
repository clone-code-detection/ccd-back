package github.clone_code_detection.exceptions.moodle;

import github.clone_code_detection.exceptions.ExceptionBase;
import jakarta.validation.constraints.NotNull;

import java.net.URI;

public class MoodleAuthenticationException extends ExceptionBase {
    private static final URI uri = URI.create("https://www.baeldung.com/articles");

    public MoodleAuthenticationException(@NotNull String message) {
        super(uri, message);
    }
}
