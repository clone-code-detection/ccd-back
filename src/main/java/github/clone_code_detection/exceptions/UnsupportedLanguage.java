package github.clone_code_detection.exceptions;

import java.net.URI;

public class UnsupportedLanguage extends ExceptionBase {
    private static final URI uri = URI.create("https://www.baeldung.com/articles");

    public UnsupportedLanguage(String message) {
        super(uri, message);
    }
}
