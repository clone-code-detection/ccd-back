package github.clone_code_detection.exceptions.highlight;

import github.clone_code_detection.exceptions.ExceptionBase;

import javax.annotation.Nullable;
import java.net.URI;

public class ElasticsearchMultiHighlightException extends ExceptionBase {
    private static final URI uri = URI.create("https://www.baeldung.com/articles");
    public ElasticsearchMultiHighlightException(String message, @Nullable Throwable cause) {
        super(uri, message, cause);
    }
}
