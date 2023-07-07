package github.clone_code_detection.exceptions.highlight;

import github.clone_code_detection.exceptions.ExceptionBase;

public class ResourceNotFoundException extends ExceptionBase {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
