package github.clone_code_detection.exceptions.highlight;

import github.clone_code_detection.exceptions.ExceptionBase;

import java.net.URI;

public class ResourceNotFoundException extends ExceptionBase {
    private final static URI uri = URI.create("about:blank");

    public ResourceNotFoundException(String message) {
        super(uri, message);
    }
}
