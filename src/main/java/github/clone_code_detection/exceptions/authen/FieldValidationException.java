package github.clone_code_detection.exceptions.authen;

import jakarta.validation.ValidationException;

public class FieldValidationException extends ValidationException {
    private final String field;

    public FieldValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return this.field;
    }
}
