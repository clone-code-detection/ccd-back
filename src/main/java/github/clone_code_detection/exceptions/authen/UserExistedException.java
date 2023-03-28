package github.clone_code_detection.exceptions.authen;

import github.clone_code_detection.exceptions.ExceptionBase;
import org.springframework.security.core.AuthenticationException;

import java.net.URI;
import java.net.URISyntaxException;

public class UserExistedException extends ExceptionBase {
    //TODO: Add documentation
    private static final URI uri = URI.create(null);

    public UserExistedException(final String msg) {
        super(uri, msg);
    }
}

