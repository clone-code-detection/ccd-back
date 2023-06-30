package github.clone_code_detection.controller.authen;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.authenication.ResetPasswordRequest;
import github.clone_code_detection.entity.authenication.SignInRequest;
import github.clone_code_detection.entity.authenication.SignUpRequest;
import github.clone_code_detection.exceptions.authen.ActivateAccountException;
import github.clone_code_detection.exceptions.authen.FieldValidationException;
import github.clone_code_detection.exceptions.authen.InvalidOperationException;
import github.clone_code_detection.exceptions.authen.ResetPasswordException;
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
import org.springframework.validation.BindException;
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
    public void signUp(@Validated SignUpRequest request, HttpServletRequest httpServletRequest) {
        assert request != null : new RuntimeException("Invalid request");
        serviceAuthentication.signUp(request, httpServletRequest);
    }

    @PostMapping(path = "/signin", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @ResponseStatus(HttpStatus.OK)
    public UserAuthenticateResponse signIn(@Validated SignInRequest request, HttpServletRequest httpServletRequest) {
        assert request != null : new RuntimeException("Invalid request");
        UserDetails userDetails = serviceAuthentication.signIn(request, httpServletRequest);
        return new UserAuthenticateResponse(userDetails);
    }

    @GetMapping(path = "/activate")
    public void activateAccount(String code) {
        serviceAuthentication.activateAccount(code);
    }

    @PostMapping(path = "/reset-password")
    public void acceptResetPassword(@Validated ResetPasswordRequest resetPasswordRequest) {
        serviceAuthentication.resetPassword(resetPasswordRequest);
    }

    @PostMapping(path = "/forget-password")
    public void resetPassword(@RequestParam String email) {
        serviceAuthentication.forgetpassword(email);
    }

    @GetMapping(path = "/info")
    @ResponseStatus(HttpStatus.OK)
    public UserAuthenticateResponse info(HttpServletRequest request) {
        UserDetails userDetails = serviceAuthentication.info(request);
        return new UserAuthenticateResponse(userDetails);
    }

    @ExceptionHandler({InvalidOperationException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ProblemDetail handleException(InvalidOperationException ex) {
        return ProblemDetailUtil.forTypeAndStatusAndDetail(
                "https://clone-code-detection.atlassian.net/wiki/spaces/CCD/pages/6914069/Authentication#Forbidden-request",
                HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(value = {AuthenticationException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ProblemDetail handleAuthentication(RuntimeException ex) {
        return ProblemDetailUtil.forTypeAndStatusAndDetail(
                "https://clone-code-detection.atlassian.net/wiki/spaces/CCD/pages/6914069/Authentication#Bad-credential",
                HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(value = {FieldValidationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleValidationException(FieldValidationException ex) {
        var field = ex.getField();
        var message = ex.getMessage();

        return ProblemDetailUtil.forTypeAndStatusAndDetail(
                "https://clone-code-detection.atlassian.net/wiki/spaces/CCD/pages/6914069/Authentication#Bad-request-"
                + field, HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(value = {BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleBadBindingException(BindException ex) {
        var fieldError = ex.getFieldError();
        String fieldName = fieldError.getField();
        String message = fieldError.getDefaultMessage();

        return ProblemDetailUtil.forTypeAndStatusAndDetail(
                "https://clone-code-detection.atlassian.net/wiki/spaces/CCD/pages/6914069/Authentication#Bad-request-"
                + fieldName, HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(value = {ActivateAccountException.class, ResetPasswordException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleOtherException(RuntimeException ex) {
        return ProblemDetailUtil.forTypeAndStatusAndDetail(
                "https://clone-code-detection.atlassian.net/wiki/spaces/CCD/pages/6914069/Authentication#Bad-request",
                HttpStatus.UNAUTHORIZED, ex.getMessage());
    }
}
