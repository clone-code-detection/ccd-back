package github.clone_code_detection.exceptions.authen;

import github.clone_code_detection.exceptions.ExceptionBase;

import java.net.URI;

public class ActivateAccountException extends ExceptionBase {
    //TODO: Add documentation
    private static final URI uri = URI.create("https://www.baeldung.com/articles");


    public ActivateAccountException(final String msg) {
        super(uri, msg);
    }
}
