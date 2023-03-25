package github.clone_code_detection.exceptions.authen;

import org.springframework.security.core.AuthenticationException;

public class UserExistedException extends AuthenticationException {

    public UserExistedException(final String msg) {
        super(msg);
    }
}

