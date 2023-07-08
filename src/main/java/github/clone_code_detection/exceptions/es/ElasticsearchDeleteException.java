package github.clone_code_detection.exceptions.es;

import github.clone_code_detection.exceptions.ExceptionBase;

public class ElasticsearchDeleteException extends ExceptionBase {
    public ElasticsearchDeleteException(String message) {
        super(message);
    }
}
