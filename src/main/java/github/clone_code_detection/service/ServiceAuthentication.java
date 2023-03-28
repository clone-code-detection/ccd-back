package github.clone_code_detection.service;

import github.clone_code_detection.entity.authenication.SignInRequest;
import github.clone_code_detection.entity.authenication.SignUpRequest;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.exceptions.authen.UserExistedException;
import github.clone_code_detection.repo.RepoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;

@Service
@Transactional
public class ServiceAuthentication {
    private final PasswordEncoder passwordEncoder;
    private final RepoUser repo;
    private final AuthenticationManager authenticationManager;


    @Autowired
    public ServiceAuthentication(PasswordEncoder passwordEncoder, RepoUser repo,
                                 AuthenticationManager authenticationManager) {
        this.passwordEncoder = passwordEncoder;
        this.repo = repo;
        this.authenticationManager = authenticationManager;
    }

    public boolean usernameExists(String username) {
        return repo.findUserByName(username) != null;
    }

    public UserImpl signIn(SignInRequest request) {
        Authentication authentication = authenticationManager.authenticate(request.toUsernamePasswordToken());
        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);
        return (UserImpl) authentication.getPrincipal();
    }

    public UserImpl create(SignUpRequest request) {
        assert request.getPassword()
                      .equals(request.getRepeat()) : "Repeat password field does not match";

        if (this.usernameExists(request.getUsername()))
            throw new UserExistedException(MessageFormat.format("User {0} already existed", request.getPassword()));
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        // Create new user's account
        UserImpl user = UserImpl.builder()
                                .username(request.getUsername())
                                .password(encodedPassword)
                                .build();
        if (request.getIsStandalone()) repo.createStandaloneUser(user);
        else repo.createOrgUser(user);
        return user;
    }
}