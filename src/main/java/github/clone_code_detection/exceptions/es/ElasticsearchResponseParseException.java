package github.clone_code_detection.exceptions.es;

import github.clone_code_detection.exceptions.ExceptionBase;

import java.net.URI;

public class ElasticsearchResponseParseException extends ExceptionBase {
    // TODO: documentation
    private static final URI uri = URI.create("https://www.baeldung.com/articles");

    public ElasticsearchResponseParseException(String message, Throwable throwable) {
        super(uri, message);
    }
}
