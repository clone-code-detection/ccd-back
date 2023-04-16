package github.clone_code_detection.exceptions.highlight;

import github.clone_code_detection.exceptions.ExceptionBase;

import java.net.URI;

public class ElasticsearchQueryException extends ExceptionBase {
    private final static URI uri = URI.create("https://www.baeldung.com/articles");

    public ElasticsearchQueryException(String message) {
        super(uri, message);
    }
}
