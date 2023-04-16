package github.clone_code_detection.exceptions.highlight;


import github.clone_code_detection.exceptions.ExceptionBase;

import java.net.URI;

public class FileNotSupportedException extends ExceptionBase {
    // TODO adds documentation
    private final static URI uri = URI.create("https://www.baeldung.com/articles");

    public FileNotSupportedException(String message) {
        super(uri, message);
    }
}
