package github.clone_code_detection.exceptions.moodle;

import github.clone_code_detection.exceptions.ExceptionBase;

import java.net.URI;

public class MoodleCourseException extends ExceptionBase {
    private static final URI uri = URI.create("https://www.baeldung.com/articles");
    public MoodleCourseException(String message) {
        super(uri, message);
    }
}
