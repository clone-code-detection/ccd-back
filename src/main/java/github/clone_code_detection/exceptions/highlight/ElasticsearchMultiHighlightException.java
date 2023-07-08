package github.clone_code_detection.exceptions.highlight;

import github.clone_code_detection.exceptions.ExceptionBase;

import javax.annotation.Nullable;

public class ElasticsearchMultiHighlightException extends ExceptionBase {
    public ElasticsearchMultiHighlightException(String message, @Nullable Throwable cause) {
        super(message, cause);
    }
}
