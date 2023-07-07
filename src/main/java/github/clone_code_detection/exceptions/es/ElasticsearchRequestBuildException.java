package github.clone_code_detection.exceptions.es;

import github.clone_code_detection.exceptions.ExceptionBase;

public class ElasticsearchRequestBuildException extends ExceptionBase {
    public ElasticsearchRequestBuildException(String message) {
        super(message);
    }
}
