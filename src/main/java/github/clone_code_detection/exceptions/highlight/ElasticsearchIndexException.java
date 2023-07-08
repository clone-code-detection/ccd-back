package github.clone_code_detection.exceptions.highlight;

import github.clone_code_detection.exceptions.ExceptionBase;

public class ElasticsearchIndexException extends ExceptionBase {
    public ElasticsearchIndexException(String message) {
        super(message);
    }
}
