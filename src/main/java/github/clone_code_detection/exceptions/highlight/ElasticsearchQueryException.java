package github.clone_code_detection.exceptions.highlight;

import github.clone_code_detection.exceptions.ExceptionBase;

public class ElasticsearchQueryException extends ExceptionBase {
    public ElasticsearchQueryException(String message) {
        super(message);
    }
}
