package github.clone_code_detection.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.ResponseUnified;
import github.clone_code_detection.entity.authenication.SignInRequest;
import github.clone_code_detection.entity.authenication.SignUpRequest;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.service.ServiceAuthentication;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.bind.annotation.*;

import javax.print.attribute.standard.Media;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@RestController
@RequestMapping("/authentication")
public class AuthenticationController implements AuthenticationEntryPoint {
    private final ServiceAuthentication serviceAuthentication;

    @Autowired
    public AuthenticationController(AuthenticationManager authenticationManager,
                                    ServiceAuthentication serviceAuthentication) {
        this.serviceAuthentication = serviceAuthentication;
    }

    private static class UserAuthenticateResponse {
        @JsonProperty("user")
        private String username;
        @JsonProperty("authenticated_at")
        private ZonedDateTime authenticated;

        public UserAuthenticateResponse(UserDetails userDetails) {
            this.username = userDetails.getUsername();
            this.authenticated = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        }
    }

    @RequestMapping(path = "/signup", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResponseUnified<UserAuthenticateResponse>> signUp(@Valid SignUpRequest request) {
        assert request != null : new RuntimeException("Invalid request");
        UserImpl user = serviceAuthentication.create(request);
        UserAuthenticateResponse authenticateResponse = new UserAuthenticateResponse(user);
        return ResponseEntity.ok(ResponseUnified.<UserAuthenticateResponse>builder()
                                                .message("success")
                                                .data(authenticateResponse)
                                                .build());
    }

    @RequestMapping(path = "/signin", method = RequestMethod.POST, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<ResponseUnified<UserAuthenticateResponse>> signIn(@Valid SignInRequest request) {
        assert request != null : new RuntimeException("Invalid request");
        UserDetails userDetails = serviceAuthentication.signIn(request);
        UserAuthenticateResponse authenticateResponse = new UserAuthenticateResponse(userDetails);
        ResponseUnified<UserAuthenticateResponse> success = ResponseUnified.<UserAuthenticateResponse>builder()
                                                                           .message("success")
                                                                           .code(0)
                                                                           .data(authenticateResponse)
                                                                           .build();
        return ResponseEntity.ok(success);
    }

    /**
     * @param request
     * @param response
     * @param authException
     * @throws IOException
     * @throws ServletException
     * @implNote function for raising error on bad request
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase());
    }
}
