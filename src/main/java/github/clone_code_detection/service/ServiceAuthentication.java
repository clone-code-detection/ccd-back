package github.clone_code_detection.service;

import github.clone_code_detection.entity.authenication.SignUpRequest;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.repo.RepoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ServiceAuthentication {
    private final PasswordEncoder passwordEncoder;
    private RepoUser repo;

    @Autowired
    public ServiceAuthentication(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public boolean usernameExists(String username) {
        return true;
    }

    public UserImpl create(SignUpRequest request) {
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        // Create new user's account
        UserImpl user = UserImpl.builder()
                                .username(request.getUsername())
                                .password(encodedPassword)
                                .build();
        repo.create(user);
        return user;
    }
}
