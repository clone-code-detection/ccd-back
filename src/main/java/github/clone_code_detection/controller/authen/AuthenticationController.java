package github.clone_code_detection.controller.authen;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.authenication.SignInRequest;
import github.clone_code_detection.entity.authenication.SignUpRequest;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.exceptions.authen.UserExistedException;
import github.clone_code_detection.service.user.ServiceAuthentication;
import github.clone_code_detection.util.ProblemDetailUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collection;

@Slf4j
@RestController
@Profile("security")
@RequestMapping("/api/authentication")
public class AuthenticationController {
    private final ServiceAuthentication serviceAuthentication;

    @Autowired
    public AuthenticationController(ServiceAuthentication serviceAuthentication) {
        this.serviceAuthentication = serviceAuthentication;
    }

    public static class UserAuthenticateResponse {
        @JsonProperty("user")
        private String username;
        @JsonProperty("authorities")
        private Collection<String> authorities;
        @JsonProperty("authenticated_at")
        private ZonedDateTime authenticated;

        public UserAuthenticateResponse(UserDetails userDetails) {
            this.username = userDetails.getUsername();
            this.authorities = userDetails.getAuthorities()
                                          .stream()
                                          .map(GrantedAuthority::getAuthority)
                                          .filter(s -> s.startsWith("ROLE_"))
                                          .map(s -> s.replaceFirst("ROLE_", ""))
                                          .toList();
            this.authenticated = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }

    @PostMapping(path = "/signup", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.CREATED)
    public UserAuthenticateResponse signUp(@Validated SignUpRequest request, HttpServletRequest httpServletRequest) {
        assert request != null : new RuntimeException("Invalid request");
        UserImpl user = serviceAuthentication.create(request, httpServletRequest);
        return new UserAuthenticateResponse(user);
    }

    @PostMapping(path = "/signin", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public UserAuthenticateResponse signIn(@Validated SignInRequest request, HttpServletRequest httpServletRequest) {
        assert request != null : new RuntimeException("Invalid request");
        UserDetails userDetails = serviceAuthentication.signIn(request, httpServletRequest);
        return new UserAuthenticateResponse(userDetails);
    }

    @GetMapping(path = "/info")
    @ResponseStatus(HttpStatus.OK)
    public UserAuthenticateResponse info(HttpServletRequest request) {
        UserDetails userDetails = serviceAuthentication.info(request);
        return new UserAuthenticateResponse(userDetails);
    }

    @ExceptionHandler({UserExistedException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleException(UserExistedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(value = {AuthenticationException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleAuthentication(RuntimeException ex) {
        return ProblemDetailUtil.forTypeAndStatusAndDetail(
                "https://clone-code-detection.atlassian.net/wiki/spaces/CCD/pages/6914069/Authentication#Bad-credential",
                HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

}
