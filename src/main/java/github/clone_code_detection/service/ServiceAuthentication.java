package github.clone_code_detection.service;

import github.clone_code_detection.entity.authenication.SignInRequest;
import github.clone_code_detection.entity.authenication.SignUpRequest;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.exceptions.authen.UserExistedException;
import github.clone_code_detection.repo.RepoUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
        try {
            repo.findUserByName(username);
        } catch (UsernameNotFoundException ignore) {
            return false;
        }
        return true;
    }

    // https://stackoverflow.com/questions/5428654/spring-security-auto-login-not-persisted-in-httpsession
    public UserImpl signIn(SignInRequest request, HttpServletRequest httpServletRequest) {
        Authentication authentication = authenticationManager.authenticate(request.toUsernamePasswordToken());
        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);
        httpServletRequest.getSession()
                          .setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                                        SecurityContextHolder.getContext());
        return (UserImpl) authentication.getPrincipal();
    }

    public UserImpl create(SignUpRequest request) {
        assert request.getPassword()
                      .equals(request.getRepeat()) : "Repeat password field does not match";

        if (this.usernameExists(request.getUsername()))
            throw new UserExistedException(MessageFormat.format("User {0} already existed", request.getUsername()));
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        // Create new user's account
        UserImpl user = UserImpl.builder()
                                .username(request.getUsername())
                                .password(encodedPassword)
                                .build();
        if (request.getIsStandalone()) user = repo.createStandaloneUser(user);
        else user = repo.createOrgUser(user);
        return user;
    }
}