package github.clone_code_detection.exceptions.es;

import github.clone_code_detection.exceptions.ExceptionBase;

import java.net.URI;

public class ElasticsearchRequestBuildException extends ExceptionBase {
    private static final URI uri = URI.create("https://www.baeldung.com/articles");

    public ElasticsearchRequestBuildException(String message) {
        super(uri, message);
    }
}
