package github.clone_code_detection.exceptions.es;

import github.clone_code_detection.exceptions.ExceptionBase;

import java.net.URI;

public class ElasticsearchDeleteException extends ExceptionBase {
    private static final URI uri = URI.create("https://www.baeldung.com/articles");

    public ElasticsearchDeleteException(String message) {
        super(uri, message);
    }
}
