package github.clone_code_detection.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import github.clone_code_detection.entity.ResponseUnified;
import github.clone_code_detection.entity.authenication.SignInRequest;
import github.clone_code_detection.entity.authenication.SignUpRequest;
import github.clone_code_detection.entity.authenication.UserImpl;
import github.clone_code_detection.service.ServiceAuthentication;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@RestController
@RequestMapping("/authentication")
public class AuthenticationController {
    private final AuthenticationManager authenticationManager;
    private final ServiceAuthentication serviceAuthentication;
    private PasswordEncoder passwordEncoder;

    @Autowired
    public AuthenticationController(AuthenticationManager authenticationManager,
                                    ServiceAuthentication serviceAuthentication, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.serviceAuthentication = serviceAuthentication;
        this.passwordEncoder = passwordEncoder;
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

    @RequestMapping(path = "/signup", method = RequestMethod.POST)
    public ResponseEntity<ResponseUnified<UserAuthenticateResponse>> signUp(@Valid SignUpRequest request) {
        assert request != null : new RuntimeException("Invalid request");
        if (serviceAuthentication.usernameExists(request.getUsername())) {
            ResponseUnified<UserAuthenticateResponse> failed = ResponseUnified.<UserAuthenticateResponse>builder()
                                                                              .message("Failed")
                                                                              .code(-1)
                                                                              .data(null)
                                                                              .build();
            return ResponseEntity.badRequest()
                                 .body(failed);
        }
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
        Authentication authentication = authenticationManager.authenticate(request.toUsernamePasswordToken());
        SecurityContextHolder.getContext()
                             .setAuthentication(authentication);
        UserImpl userDetails = (UserImpl) authentication.getPrincipal();
        UserAuthenticateResponse authenticateResponse = new UserAuthenticateResponse(userDetails);
        return ResponseEntity.ok(ResponseUnified.<UserAuthenticateResponse>builder()
                                                .message("success")
                                                .data(authenticateResponse)
                                                .build());
    }
//    /**
//     * @param request
//     * @param response
//     * @param authException
//     * @throws IOException
//     * @throws ServletException
//     * @implNote function for raising error on bad request
//     */
//    @Override
//    public void commence(HttpServletRequest request, HttpServletResponse response,
//                         AuthenticationException authException) throws IOException, ServletException {
//        response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase());
//    }
}
