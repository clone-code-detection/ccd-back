package github.clone_code_detection.exceptions.moodle;

import github.clone_code_detection.exceptions.ExceptionBase;

import java.net.URI;

public class MoodleAssignmentException extends ExceptionBase {
    private static final URI uri = URI.create("https://www.baeldung.com/articles");

    public MoodleAssignmentException(String message) {
        super(uri, message);
    }
}
