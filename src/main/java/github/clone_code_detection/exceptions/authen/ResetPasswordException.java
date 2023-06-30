package github.clone_code_detection.exceptions.authen;


import github.clone_code_detection.exceptions.ExceptionBase;

import java.net.URI;

public class ResetPasswordException extends ExceptionBase {
    //TODO: Add documentation
    private static final URI uri = URI.create("https://www.baeldung.com/articles");


    public ResetPasswordException(final String msg) {
        super(uri, msg);
    }
}