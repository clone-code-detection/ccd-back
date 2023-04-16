package github.clone_code_detection.exceptions.es.highlight;

import github.clone_code_detection.exceptions.ExceptionBase;

import java.net.URI;

public class HighlightLanguageException extends ExceptionBase {
    //TODO: Add documentation
    private static final URI uri = URI.create(null);

    public HighlightLanguageException(String msg) {
        super(uri, msg);
    }
}
